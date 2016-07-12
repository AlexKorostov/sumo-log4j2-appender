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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * Appender that sends log messages to Sumo Logic.
 *
 * @author Stefan Zier (stefan@sumologic.com)
 */
@Plugin(name = "SumoLogic", category = "Core", elementType = "appender", printObject = true)
public class SumoLogicAppender extends AbstractAppender {

  private String url;
  private int connectionTimeout;
  private int socketTimeout;

  private HttpClient httpClient = null;

  public void setUrl(String url) {
    this.url = url;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  private SumoLogicAppender(String name, Layout layout, Filter filter, boolean ignoreExceptions) {
    super(name, filter, layout, ignoreExceptions);
  }

  @PluginFactory
  public static SumoLogicAppender createAppender(@PluginAttribute("name") String name,
                                            @PluginAttribute("url") String url,
                                            @PluginAttribute(value = "socketTimeout", defaultInt = 60000) int socketTimeout,
                                            @PluginAttribute(value = "connectionTimeout", defaultInt = 1000) int connectionTimeout,
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
    SumoLogicAppender appender = new SumoLogicAppender(name, layout, filter, ignoreExceptions);
    appender.setUrl(url);
    appender.setSocketTimeout(socketTimeout);
    appender.setConnectionTimeout(connectionTimeout);
    return appender;
  }

  @Override
  public void start() {
    super.start();
    HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(params, connectionTimeout);
    HttpConnectionParams.setSoTimeout(params, socketTimeout);
    httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(), params);
  }


  @Override
  public void append(LogEvent event) {
    if (!checkEntryConditions()) {
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

    sendToSumo(builder.toString());
  }

  @Override
  public void stop() {
    super.stop();
    httpClient.getConnectionManager().shutdown();
    httpClient = null;
  }

  // Private bits.

  private boolean checkEntryConditions() {
    if (httpClient == null) {
      LOGGER.warn("HttpClient not initialized.");
      return false;
    }

    return true;
  }

  private void sendToSumo(String log) {
    HttpPost post = null;
    try {
      post = new HttpPost(url);
      post.setEntity(new StringEntity(log, HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(post);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        LOGGER.warn(String.format("Received HTTP error from Sumo Service: %d", statusCode));
      }
      //need to consume the body if you want to re-use the connection.
      EntityUtils.consume(response.getEntity());
    } catch (IOException e) {
      LOGGER.warn("Could not send log to Sumo Logic", e);
      try { post.abort(); } catch (Exception ignore) {}
    }
  }
}
