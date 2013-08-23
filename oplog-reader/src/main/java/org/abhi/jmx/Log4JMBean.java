package org.abhi.jmx;

import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Class which allows managing log4j logging levels. This class is meant to be exported by Spring (
 * {@link MBeanExporter}) as an MBean to an MBean Server. This effectively allows you to dynamically update
 * logging levels via JMX
 * 
 * @author Abhinav Shah
 * 
 */
@ManagedResource(objectName = "abhi,mgmt.logs:name=LoggingLevelHander", description = "MBean Used to update logging levels at runtime")
public class Log4JMBean {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Log4JMBean.class);

	@ManagedOperation(description = "Sets the logging level for a given Logger.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "Logger Name", description = "Name of Logger to update."),
			@ManagedOperationParameter(name = "Level", description = "Logging Level to set.") })
	public void setLogLevel(String loggerName, String level) {
		Level logLevel = Level.toLevel(level, Level.WARN);
		Logger.getLogger(loggerName).setLevel(logLevel);
		LOGGER.warn("Changed " + loggerName + " level to: " + level);
	}

	@ManagedOperation(description = "Sets the logging level of the root logger.")
	@ManagedOperationParameters({ @ManagedOperationParameter(name = "Level", description = "Logging Level to set.") })
	public void setRootLogLevel(String level) {
		Level logLevel = Level.toLevel(level, Level.WARN);
		Logger.getRootLogger().setLevel(logLevel);
		LOGGER.warn("Changed root logger level to: " + level);

	}

	@ManagedAttribute(description = "Retrieves the list of existing loggers and their current level.")
	public Set<String> getLoggerList() {
		LOGGER.warn("Getting list of loggers");
		Set<String> loggerList = new TreeSet<String>();
		@SuppressWarnings("unchecked")
		Enumeration<Logger> loggers = LogManager.getCurrentLoggers();
		Logger logger = null;
		while (loggers.hasMoreElements()) {
			logger = loggers.nextElement();
			loggerList.add(logger.getName() + " - " + logger.getLevel());
		}
		return loggerList;
	}

}
