/**
 *    _____ _____ _____ _____    __    _____ _____ _____ _____
 *   |   __|  |  |     |     |  |  |  |     |   __|     |     |
 *   |__   |  |  | | | |  |  |  |  |__|  |  |  |  |-   -|   --|
 *   |_____|_____|_|_|_|_____|  |_____|_____|_____|_____|_____|
 *
 *                UNICORNS AT WARP SPEED SINCE 2010
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.sumologic.log4j;

import com.sumologic.log4j.aggregation.SumoBufferFlusher;
import com.sumologic.log4j.http.ProxySettings;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;
import com.sumologic.log4j.queue.BufferWithFifoEviction;
import org.apache.http.util.ExceptionUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;

import static com.sumologic.log4j.queue.CostBoundedConcurrentQueue.CostAssigner;

/**
 * Appender that sends log messages to Sumo Logic.
 *
 * @author Jose Muniz (jose@sumologic.com)
 */
@Plugin(name = "BufferedSumoLogic", category = "Core", elementType = "appender", printObject = true)
public class BufferedSumoLogicAppender extends AbstractAppender {

    private String url;

    private String proxyHost;
    private int proxyPort;
    private String proxyAuth;
    private String proxyUser;
    private String proxyPassword;
    private String proxyDomain;


    private int connectionTimeout;
    private int socketTimeout;
    private int retryInterval;        // Once a request fails, how often until we retry.

    private long messagesPerRequest;    // How many messages need to be in the queue before we flush
    private long maxFlushInterval;    // Maximum interval between flushes (ms)
    private long flushingAccuracy;      // How often the flushed thread looks into the message queue (ms)
    private String sourceName; // Name to stamp for querying with _sourceName

    private long maxQueueSizeBytes;

    private SumoHttpSender sender;
    private SumoBufferFlusher flusher;
    volatile private BufferWithEviction<String> queue;

    private BufferedSumoLogicAppender(String name, Layout layout, Filter filter, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @PluginFactory
    public static BufferedSumoLogicAppender createAppender(@PluginAttribute("name") String name,
                                                           @PluginAttribute("url") String url,
                                                           @PluginAttribute(value = "sourceName", defaultString = "Log4J-SumoObject") String sourceName,
                                                           @PluginAttribute(value = "socketTimeout", defaultInt = 60000) int socketTimeout,
                                                           @PluginAttribute(value = "connectionTimeout", defaultInt = 1000) int connectionTimeout,
                                                           @PluginAttribute(value = "retryInterval",defaultInt = 10000) int retryInterval,
                                                           @PluginAttribute(value = "flushingAccuracy", defaultInt = 250) int flushingAccuracy,
                                                           @PluginAttribute(value = "maxFlushInterval", defaultInt = 10000) int maxFlushInterval,
                                                           @PluginAttribute(value = "messagesPerRequest", defaultInt = 100) int messagesPerRequest,
                                                           @PluginAttribute(value = "maxQueueSizeBytes", defaultInt = 1000000) int maxQueueSizeBytes,
                                                           @PluginAttribute("proxyHost") String proxyHost,
                                                           @PluginAttribute(value = "proxyPort", defaultInt = -1) int proxyPort,
                                                           @PluginAttribute("proxyAuth") String proxyAuth,
                                                           @PluginAttribute("proxyUser") String proxyUser,
                                                           @PluginAttribute("proxyPassword") String proxyPassword,
                                                           @PluginAttribute("proxyDomain") String proxyDomain,
                                                           @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                           @PluginElement("Layout") Layout layout,
                                                           @PluginElement("Filters") Filter filter) {

        if (name == null) {
            LOGGER.error("No name provided for SumoLogicAppender");
            return null;
        }
        if (url == null) {
            LOGGER.error("No url provided for SumoLogicAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        BufferedSumoLogicAppender appender = new BufferedSumoLogicAppender(name, layout, filter, ignoreExceptions);
        appender.setUrl(url);
        if (sourceName != null) {
            appender.setSourceName(sourceName);
        }
        appender.setSocketTimeout(socketTimeout);
        appender.setConnectionTimeout(connectionTimeout);
        appender.setRetryInterval(retryInterval);
        appender.setFlushingAccuracy(flushingAccuracy);
        appender.setMaxFlushInterval(maxFlushInterval);
        appender.setMessagesPerRequest(messagesPerRequest);
        appender.setMaxQueueSizeBytes(maxQueueSizeBytes);
        appender.setProxyPort(proxyPort);
        if (proxyHost != null)
            appender.setProxyHost(proxyHost);
        if (proxyAuth != null)
            appender.setProxyAuth(proxyAuth);
        if (proxyUser != null)
            appender.setProxyUser(proxyUser);
        if (proxyPassword != null)
            appender.setProxyPassword(proxyPassword);
        if (proxyDomain != null)
            appender.setProxyDomain(proxyDomain);
        return appender;
    }

    /* All the parameters */

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMaxQueueSizeBytes(long maxQueueSizeBytes) {
        this.maxQueueSizeBytes = maxQueueSizeBytes;
    }

    public void setMessagesPerRequest(long messagesPerRequest) {
        this.messagesPerRequest = messagesPerRequest;
    }


    public void setMaxFlushInterval(long maxFlushInterval) {
        this.maxFlushInterval = maxFlushInterval;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void setFlushingAccuracy(long flushingAccuracy) {
        this.flushingAccuracy = flushingAccuracy;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyAuth() {
        return proxyAuth;
    }

    public void setProxyAuth(String proxyAuth) {
        this.proxyAuth = proxyAuth;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getProxyDomain() {
        return proxyDomain;
    }

    public void setProxyDomain(String proxyDomain) {
        this.proxyDomain = proxyDomain;
    }

    @Override
    public void start() {
        super.start();
        StatusLogger.getLogger().debug("Activating options");

        /* Initialize queue */
        if (queue == null) {
            queue = new BufferWithFifoEviction<String>(maxQueueSizeBytes, new CostAssigner<String>() {
              @Override
              public long cost(String e) {
                  // Note: This is only an estimate for total byte usage, since in UTF-8 encoding,
                  // the size of one character may be > 1 byte.
                 return e.length();
              }
            });
        } else {
            queue.setCapacity(maxQueueSizeBytes);
        }

        /* Initialize sender */
        if (sender == null)
            sender = new SumoHttpSender();

        sender.setRetryInterval(retryInterval);
        sender.setConnectionTimeout(connectionTimeout);
        sender.setSocketTimeout(socketTimeout);
        sender.setUrl(url);
        sender.setProxySettings(new ProxySettings(
                proxyHost,
                proxyPort,
                proxyAuth,
                proxyUser,
                proxyPassword,
                proxyDomain));

        sender.init();

        /* Initialize flusher  */
        if (flusher != null)
            flusher.stop();

        flusher = new SumoBufferFlusher(flushingAccuracy,
                    messagesPerRequest,
                    maxFlushInterval,
                    sourceName,
                    sender,
                    queue);
        flusher.start();

    }

    @Override
    public void append(LogEvent event) {
        if (!checkEntryConditions()) {
            StatusLogger.getLogger().warn("Appender not initialized. Dropping log entry");
            return;
        }

        StringBuilder builder = new StringBuilder(1024);
        builder.append(getLayout().toSerializable(event));
        if (ignoreExceptions() && event.getThrown() != null) {
            for (String line : Throwables.toStringList(event.getThrown())) {
                builder.append(line);
                builder.append("\n");
            }
        }

        try {
            queue.add(builder.toString());
        } catch (Exception e) {
            StatusLogger.getLogger().error("Unable to insert log entry into log queue. ", e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        try {
            sender.close();
            sender = null;

            flusher.stop();
            flusher = null;
        } catch (IOException e) {
            StatusLogger.getLogger().error("Unable to close appender", e);
        }
    }


    // Private bits.

    private boolean checkEntryConditions() {
        return sender != null && sender.isInitialized();
    }

}
