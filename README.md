sumo-log4j-appender
===================

A Log4J appender that sends straight to Sumo Logic.

Usage
-----

Here is a sample log4.properties file. Make sure to replace [collector-url] with the URL from the Sumo Logic UI.

    <Configuration status="WARN">
      <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
          <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <BufferedSumoLogic name="SumoLogicLog" url="<your collection url>"
                   socketTimeout="12003">
          <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n" charset="UTF-8"/>
        </BufferedSumoLogic>
      </Appenders>
      <Loggers>
        <Root level="info">
          <AppenderRef ref="Console"/>
          <AppenderRef ref="SumoLogicLog"/>
        </Root>
      </Loggers>
    </Configuration>

Supported optional attributes:
- sourceName, default "Log4J-SumoObject"
- socketTimeout, default 60000
- connectionTimeout, default 1000
- retryInterval, default 10000
- flushingAccuracy, default 250
- maxFlushInterval, default 10000
- messagesPerRequest, default 100
- maxQueueSizeBytes, default 1000000
- proxyHost, no default
- proxyHost, no default
- proxyPort, no default
- proxyAuth, no default
- proxyUser, no default
- proxyPassword, no default
- proxyDomain, no default



To build:
- Run "mvn clean package" on the pom.xml in the main level of this project.
- The pom is packaging all of the dependent JAR files into one massive jar file called "uber-sumo-log4j-appender-1.0-SNAPSHOT.jar". If you do not want all of this, remove the following XML from the pom.xml file:

	<build>
		<plugins>
			<plugin>
			    <groupId>org.apache.maven.plugins</groupId>
			    <artifactId>maven-shade-plugin</artifactId>
			    <executions>
			        <execution>
			            <phase>package</phase>
			            <goals>
			                <goal>shade</goal>
			            </goals>
			        </execution>
			    </executions>
			    <configuration>
			        <finalName>uber-${artifactId}-${version}</finalName>
			    </configuration>
			</plugin>
		</plugins>
	</build>

To run this as a stand alone Java application:
- create a Java main, follow "com.sumologic.log4j.SumoLogicAppenderExample".
- place the log4j.properties file under "/src/main/resources/"
- if you created a main called "com.sumologic.log4j.SumoLogicAppenderExample", 
then run: "java -cp target/sumo-log4j2-appender-0.1.jar com.sumologic.log4j.SumoLogicAppenderExample" to see it in action.

To run this as web application make sure the log4j.properties file is in the classpath. In many cases you will want it in your "WEB-INF/lib" folder.
 
