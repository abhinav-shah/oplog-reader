package org.abhi.mongodb.oplogreader;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;

@ManagedResource(objectName = "abhi.mgmt.oplogreader:name=OplogReaderManager", description = "MBean Used to manage oplog reader")
public class OplogReaderLauncher implements InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(OplogReader.class);

	@Value("#{ systemProperties['replica.sets'] }")
	private String replicaSets;

	@Autowired
	@Qualifier("oplogReaderTaskExecutor")
	private TaskExecutor taskExecutor;

	private Map<String, OplogReader> oplogReaderMap = new HashMap<String, OplogReader>();

	public void afterPropertiesSet() throws Exception {
		LOGGER.debug("********   Inside OplogReaderLauncher.afterPropertiesSet");
		try {
			String[] replicaSetArray = replicaSets.split(";");
			for (String r : replicaSetArray) {
				OplogReader oplogReader = new OplogReader(r);
				oplogReaderMap.put(r, oplogReader);
			}
			for (String k : oplogReaderMap.keySet()) {
				this.taskExecutor.execute(oplogReaderMap.get(k));
			}
		} catch (Exception e) {
			LOGGER.error("Exception in method afterPropertiesSet", e);
			throw e;
		}
	}

	public void destroy() throws Exception {
		LOGGER.debug("********   Inside OplogReaderLauncher.destroy");
		for (String k : oplogReaderMap.keySet()) {
			oplogReaderMap.get(k).stopTailing();
		}
		oplogReaderMap.clear();
	}

	@ManagedOperation(description = "Starts oplogreaders")
	public void start() throws Exception {
		stop();
		afterPropertiesSet();
	}

	@ManagedOperation(description = "Stops oplogreaders")
	public void stop() throws Exception {
		try {
			for (String k : oplogReaderMap.keySet()) {
				oplogReaderMap.get(k).stopTailing();
			}
			oplogReaderMap.clear();
		} catch (Exception e) {
			LOGGER.error("Exception in method stop", e);
			throw e;
		}
	}

	@ManagedAttribute(description = "Check if all oplog readers are running")
	public boolean isRunning() {
		boolean isRunning = true;
		if (oplogReaderMap == null || oplogReaderMap.keySet() == null || oplogReaderMap.keySet().size() == 0) {
			return false;
		}
		for (String k : oplogReaderMap.keySet()) {
			OplogReader o = oplogReaderMap.get(k);
			if (!o.isRunning()) {
				return false;
			}
		}
		return isRunning;
	}

	@Scheduled(cron = "0 */2 * * * *")
	public void checkAndStartOplogReader() throws Exception {
		LOGGER.debug("OplogReaderLauncher Cron running..");
		if (!isRunning()) {
			LOGGER.debug("OplogReaderLauncher Cron starting OplogReader..");
			start();
		}
	}

}
