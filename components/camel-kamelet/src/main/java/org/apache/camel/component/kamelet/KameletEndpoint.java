/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.kamelet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProducer;
import org.apache.camel.Consumer;
import org.apache.camel.DelegateEndpoint;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultAsyncProducer;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;

@UriEndpoint(
    firstVersion = "3.5.0",
    scheme = "kamelet",
    syntax = "kamelet:templateId/routeId",
    title = "Kamelet",
    lenientProperties = true,
    label = "camel-k")
public class KameletEndpoint extends DefaultEndpoint implements DelegateEndpoint {
    @Metadata(required = true)
    @UriPath(description = "The Route Template ID")
    private final String templateId;

    @Metadata(required = false)
    @UriPath(description = "The Route ID", defaultValueNote = "The ID will be auto-generated if not provided")
    private final String routeId;

    private final Map<String, Object> kameletProperties;

    private Endpoint kameletEndpoint;

    public KameletEndpoint(
            String uri,
            KameletComponent component,
            String templateId,
            String routeId) {

        super(uri, component);

        ObjectHelper.notNull(templateId, "template id");
        ObjectHelper.notNull(routeId, "route id");

        this.templateId = templateId;
        this.routeId = routeId;
        this.kameletProperties = new HashMap<>();
    }

    @Override
    public KameletComponent getComponent() {
        return (KameletComponent) super.getComponent();
    }

    @Override
    public Endpoint getEndpoint() {
        return kameletEndpoint;
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setKameletProperties(Map<String, Object> kameletProperties) {
        if (kameletProperties != null) {
            this.kameletProperties.clear();
            this.kameletProperties.putAll(kameletProperties);
        }
    }

    public Map<String, Object> getKameletProperties() {
        return Collections.unmodifiableMap(kameletProperties);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new KameletProducer();
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer answer = new KemeletConsumer(processor);
        configureConsumer(answer);
        return answer;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        getComponent().onEndpointAdd(this);
    }

    @Override
    protected void doStart() throws Exception {
        kameletEndpoint =  getCamelContext().getEndpoint("direct:" + routeId);
        ServiceHelper.startService(kameletEndpoint);

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(kameletEndpoint);
        super.doStop();
    }

    // *********************************
    //
    // Helpers
    //
    // *********************************

    private class KemeletConsumer extends DefaultConsumer {
        private volatile Consumer consumer;

        public KemeletConsumer(Processor processor) {
            super(KameletEndpoint.this, processor);
        }

        @Override
        protected void doStart() throws Exception {
            consumer = kameletEndpoint.createConsumer(getProcessor());

            ServiceHelper.startService(consumer);
            super.doStart();
        }

        @Override
        protected void doStop() throws Exception {
            ServiceHelper.stopService(consumer);
            super.doStop();
        }
    }

    private class KameletProducer extends DefaultAsyncProducer {
        private volatile AsyncProducer producer;

        public KameletProducer() {
            super(KameletEndpoint.this);
        }

        @Override
        public boolean process(Exchange exchange, AsyncCallback callback) {
            if (producer != null) {
                return producer.process(exchange, callback);
            } else {
                callback.done(true);
                return true;
            }
        }

        @Override
        protected void doStart() throws Exception {
            producer = kameletEndpoint.createAsyncProducer();
            ServiceHelper.startService(producer);
            super.doStart();
        }

        @Override
        protected void doStop() throws Exception {
            ServiceHelper.stopService(producer);
            super.doStop();
        }
    }
}