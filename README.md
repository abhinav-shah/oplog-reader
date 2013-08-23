System Requirements:
--------------------
* Java 1.6
* Maven2
* MongoDB
	

About:
------
	This is a MongoDB oplog reader which can be deployed as a Web Application in any J2EE container. 
	Once you deploy this application, the oplog reader starts automatically. 
	By default the output goes to a java.util.concurrent.LinkedBlockingDeque. 
	But this can be easily modified to send the output to anywhere you want. I typically use a IBM MQ. 
	Also, it allows for listening to multiple replica sets. It spawns one thread for each replica set. 
	You can also configure what namespaces(Collections) you want to listen to.
	Additionally, you can also control the oplog-reader using JMX. 
	
	It also has a cron running every 2 minutes to see if the oplog reader is running. 
	In case it has stopped it will try to start it.
	
Configuration:
--------------
	Following system properties need to be set before running.	
	-Dnamespaceset=[comma seperated list of namespaces]
	-Dlocal.db.name=[local database name]
	-Dlocal.db.username=[local database username]
	-Dlocal.db.password=[local database password]
	-Doplogts.file.location=[absolute path where you want to store the timestamp files for all replica sets]
	-Dreplica.sets=[; delimited replica sets]
	-Doplog.log.file.path=[path of the log file where the program logs will be written to]
	
	JMX port it specified in application.properties file. 
	JMX URL would be service:jmx:rmi://localhost/jndi/rmi://localhost:${jmx.port}/jmxconnector
	
	Following is an example - 
	
	-Dnamespaceset=pec_dev.Encounter,rc_dev.reports
	-Dlocal.db.name=local
	-Dlocal.db.username=local_dev 
	-Dlocal.db.password=local_dev
	-Doplogts.file.location=C:/ 
	-Dreplica.sets=abcd:37017,pqrs:37018,lmno:37019;jhgf:37017,rwqs:37018,dfgtd:37019;fgdfg:37017,asrtgf:37018,dfgdfg:37019
	-Doplog.log.file.path=/logs/oplog-reader.log
	
I am using 3 replica sets:	
* abcd:37017,pqrs:37018,lmno:37019
* jhgf:37017,rwqs:37018,dfgtd:37019
* fgdfg:37017,asrtgf:37018,dfgdfg:37019
		
