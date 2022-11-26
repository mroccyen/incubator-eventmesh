/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.eventmesh.connector.mongodb.producer;

import com.mongodb.*;
import io.cloudevents.CloudEvent;
import org.apache.eventmesh.api.RequestReplyCallback;
import org.apache.eventmesh.api.SendCallback;
import org.apache.eventmesh.api.producer.Producer;
import org.apache.eventmesh.connector.mongodb.client.MongodbClientStandaloneManager;
import org.apache.eventmesh.connector.mongodb.config.ConfigurationHolder;
import org.apache.eventmesh.connector.mongodb.constant.MongodbConstants;
import org.apache.eventmesh.connector.mongodb.utils.MongodbSequenceUtil;

import java.util.Properties;

public class MongodbStandaloneProcuder implements Producer {

    private final ConfigurationHolder configurationHolder;

    private volatile boolean started = false;

    private MongoClient client;

    private DB db;

    private DBCollection cappedCol;

    public MongodbStandaloneProcuder(ConfigurationHolder configurationHolder) {
        this.configurationHolder = configurationHolder;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isClosed() {
        return !isStarted();
    }

    @Override
    public void start() {
        if (!started) {
            started = true;
        }
    }

    @Override
    public void shutdown() {
        if (started) {
            try {
                MongodbClientStandaloneManager.closeMongodbClient(this.client);
            } finally {
                started = false;
            }
        }
    }

    @Override
    public void init(Properties keyValue) {
        this.configurationHolder.init();
        this.client = MongodbClientStandaloneManager.createMongodbClient(configurationHolder);
        this.db = client.getDB(configurationHolder.getDatabase());
        this.cappedCol = db.getCollection(configurationHolder.getCollection());
    }

    @Override
    public void publish(CloudEvent cloudEvent, SendCallback sendCallback) {
        int i = MongodbSequenceUtil.getInstance().getNextSeq(MongodbConstants.Topic);
        DBObject doc = new BasicDBObject()
                .append(MongodbConstants.CAPPED_COL_TOPIC_FN, MongodbConstants.Topic)
                .append(MongodbConstants.CAPPED_COL_NAME_FN, "name" + i)
                .append(MongodbConstants.CAPPED_COL_CURSOR_FN, i);
        System.out.println("publisher is going to publish number " + i + "th message");
        cappedCol.insert(doc);
    }

    @Override
    public void sendOneway(CloudEvent cloudEvent) {

    }

    @Override
    public void request(CloudEvent cloudEvent, RequestReplyCallback rrCallback, long timeout) throws Exception {

    }

    @Override
    public boolean reply(CloudEvent cloudEvent, SendCallback sendCallback) throws Exception {
        return false;
    }

    @Override
    public void checkTopicExist(String topic) throws Exception {

    }

    @Override
    public void setExtFields() {

    }
}
