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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Simple example on using the Sumo Logic Log4J appender.
 *
 * @author Stefan Zier (stefan@sumologic.com)
 */
public class SumoLogicAppenderExample {
  private static Logger logger = LogManager.getLogger(SumoLogicAppenderExample.class);

  public static void main(String[] args) throws InterruptedException {
      StatusLogger.getLogger().setLevel(Level.DEBUG);


      long start = System.currentTimeMillis();
      for (int i = 0; i < 5; i++) {

          for (int j = 0; j < 100; j++)
            logger.error("Greetings from the SumoLogicAppender!");

          logger.error("Multiline message", new RuntimeException());
          Thread.sleep(100);
      }
    System.out.println("Elapsed time " + (System.currentTimeMillis() - start) + " ms");
    Thread.sleep(5000);
  }
}
