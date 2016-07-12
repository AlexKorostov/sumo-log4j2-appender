sumo-log4j2-appender
===================

A Log4J appender that sends straight to Sumo Logic.

Usage
-----

Here is a sample log4j2.xml file. Make sure to replace [collector-url] with the URL from the Sumo Logic UI.

    <Configuration status="WARN">
      <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
          <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <BufferedSumoLogic name="SumoLogicLog" url="[collector-url]"
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