package org.abhi.mongodb.oplogreader;

import java.io.File;
import java.io.FileWriter;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.bson.types.BSONTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoFactoryBean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.util.StringUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ServerAddress;

public final class OplogReader implements Runnable {

	private static final String oplogTsFileLocation = System.getProperty("oplogts.file.location");

	private static final String dbName = System.getProperty("local.db.name");

	private static final String dbUserName = System.getProperty("local.db.username");

	private static final String dbPassword = System.getProperty("local.db.password");

	private static final String namespaceCsv = System.getProperty("namespaceset");

	private boolean isRunning = false;

	private MongoFactoryBean mongoFactoryBean;
	private MongoDbFactory dbFactory;

	private MongoOperations localDbTemplate;

	private String oplogTsFilePath;

	private String replicaSet;

	private static final String OPLOG_TS_FILENAME = "oplogts-";

	private static final Logger LOGGER = LoggerFactory.getLogger(OplogReader.class);

	private Queue<DBObject> messageQueue = new LinkedBlockingDeque<DBObject>();

	public OplogReader(String replicaSet) throws Exception {
		this.replicaSet = replicaSet;
		mongoFactoryBean = new MongoFactoryBean();
		String[] addresses = replicaSet.split(",");
		ServerAddress[] replicaSetSeeds = new ServerAddress[addresses.length];
		for (int i = 0; i < addresses.length; i++) {
			replicaSetSeeds[i] = getServerAddress(addresses[i]);
		}
		mongoFactoryBean.setReplicaSetSeeds(replicaSetSeeds);
		mongoFactoryBean.afterPropertiesSet();
		dbFactory = new SimpleMongoDbFactory(mongoFactoryBean.getObject(), dbName,
				new UserCredentials(
						dbUserName, dbPassword));
		localDbTemplate = new MongoTemplate(dbFactory);
		String temp = replicaSet.replace(",", "-").replace(":", "-");
		oplogTsFilePath = oplogTsFileLocation + OPLOG_TS_FILENAME + temp
				+ ".txt";
		File f = new File(oplogTsFilePath);
		f.createNewFile();
	}

	public void run() {
		startTailing();
	}

	public void startTailing() {
		LOGGER.debug("********  Started tailing replica set: " + this.replicaSet);
		this.setRunning(true);
		this.tailOplog();
	}

	public void stopTailing() throws Exception {
		if (isRunning()) {
			LOGGER.debug("********  Stopping tailing replica set: " + this.replicaSet);
			this.setRunning(false);
			LOGGER.debug("********  Stopped");
		}
	}

	private ServerAddress getServerAddress(String hostPort) throws UnknownHostException {
		int idx = hostPort.indexOf(":");
		int port = Integer.parseInt(hostPort.substring(idx + 1));
		return new ServerAddress(hostPort.substring(0, idx), port);
	}

	private void tailOplog() {
		FileWriter tsFileWriter = null;
		try {
			DBCollection oplog = localDbTemplate.getCollection("oplog.rs");
			Resource resource = new FileSystemResource(oplogTsFilePath);
			Properties opLogTsProperties = PropertiesLoaderUtils.loadProperties(resource);

			String timestamp = opLogTsProperties.get("ts") == null ? null : (String) opLogTsProperties
					.get("ts");

			DBCursor lastCursor;
			BSONTimestamp ts = null;
			if (null == timestamp) {
				lastCursor = oplog.find().sort(new BasicDBObject("$natural", 1));
				if (!lastCursor.hasNext()) {
					LOGGER.debug("no oplog!");
					return;
				}

			} else {
				int time = Integer.valueOf(timestamp.split(",")[0]);
				int inc = Integer.valueOf(timestamp.split(",")[1]);
				ts = new BSONTimestamp(time, inc);
				lastCursor = oplog
						.find(new BasicDBObject("ts", new BasicDBObject("$gt", ts)));
			}

			Set<String> namespaceSet = StringUtils.commaDelimitedListToSet(namespaceCsv);
			while (true && isRunning()) {
				LOGGER.trace("Running..");
				lastCursor.addOption(Bytes.QUERYOPTION_TAILABLE);
				// lastCursor.addOption(Bytes.QUERYOPTION_AWAITDATA);
				while (lastCursor.hasNext() && isRunning()) {
					DBObject x = lastCursor.next();
					String ns = (String) x.get("ns");
					ts = (BSONTimestamp) x.get("ts");
					if (ns != null && namespaceSet.contains(ns)) {
						LOGGER.trace("TimeStamp" + x);
						DBObject dbObject = (DBObject) x.get("o");
						if (dbObject != null) {
							String operation = (String) x.get("op");
							if ("i".equals(operation) || "u".equals(operation) || "d".equals(operation)) {
								dbObject.put("operation", operation);
								dbObject.put("ns", x.get("ns"));
								messageQueue.add(dbObject);
								LOGGER.debug("Message queued: | " + " Namespace: " + dbObject.get("ns")
										+ " | ID: "
										+ dbObject.get("_id"));
								LOGGER.trace("Message string: " + dbObject.toString());
							}
						}
					}
					LOGGER.trace("********  Writing to file");
					tsFileWriter = new FileWriter(oplogTsFilePath, false);
					tsFileWriter.write("ts=" + String.valueOf(ts.getTime()) + ","
							+ String.valueOf(ts.getInc())
							+ System.getProperty("line.separator"));
					tsFileWriter.flush();
					tsFileWriter.close();
				}
				lastCursor.close();
				lastCursor = oplog.find(new BasicDBObject("ts", new BasicDBObject("$gt", ts)));
				Thread.sleep(100);
			}
		} catch (Exception e) {
			LOGGER.error("Exception in method tailOplog", e);
			LOGGER.error("Exception in oplogReader tailing replica set: " + this.replicaSet);
			throw new RuntimeException("Exception in method tailOplog", e.getCause());
		} finally {
			setRunning(false);
			try {
				if (null != tsFileWriter) {
					tsFileWriter.close();
				}
			} catch (Exception e) {
				LOGGER.error("Exception in method tailOplog", e);
				throw new RuntimeException("Exception in method tailOplog", e.getCause());
			}
		}
	}

	public boolean isRunning() {
		return isRunning;
	}

	private void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	@Override
	protected void finalize() throws Throwable {
		mongoFactoryBean.destroy();
		super.finalize();
	}
}
