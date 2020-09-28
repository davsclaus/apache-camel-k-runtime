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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.VetoCamelContextStartException;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.LifecycleStrategySupport;

/**
 * The Kamelet Component provides support for materializing routes templates.
 */
@Component(Kamelet.SCHEME)
public class KameletComponent extends DefaultComponent {
    private final LifecycleHandler lifecycleHandler;

    public KameletComponent() {
        this.lifecycleHandler = new LifecycleHandler();
    }

    @Override
    public Endpoint createEndpoint(String uri) throws Exception {
        switch (uri) {
            case "kamelet:source":
            case "kamelet://source":
            case "kamelet:sink":
            case "kamelet://sink":
                return getCamelContext().getEndpoint("direct:{{routeId}}");
            default:
                return super.createEndpoint(uri);
        }
    }

    @Override
    public Endpoint createEndpoint(String uri, Map<String, Object> parameters) throws Exception {
        switch (uri) {
            case "kamelet:source":
            case "kamelet://source":
            case "kamelet:sink":
            case "kamelet://sink":
                return getCamelContext().getEndpoint("direct:{{routeId}}");
            default:
                return super.createEndpoint(uri, parameters);
        }
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final String templateId = Kamelet.extractTemplateId(getCamelContext(), remaining);
        final String routeId = Kamelet.extractRouteId(getCamelContext(), remaining);
        final KameletEndpoint endpoint = new KameletEndpoint(uri, this, templateId, routeId);

        // set endpoint specific properties
        setProperties(endpoint, parameters);

        //
        // The properties for the kamelets are determined by global properties
        // and local endpoint parameters,
        //
        // Global parameters are loaded in the following order:
        //
        //   camel.kamelet." + templateId
        //   camel.kamelet." + templateId + "." routeId
        //
        Map<String, Object> kameletProperties = Kamelet.extractKameletProperties(getCamelContext(), templateId, routeId);
        kameletProperties.putAll(parameters);
        kameletProperties.put("templateId", templateId);
        kameletProperties.put("routeId", routeId);

        // set kamelet specific properties
        endpoint.setKameletProperties(kameletProperties);

        return endpoint;
    }

    @Override
    protected void doInit() throws Exception {
        getCamelContext().addLifecycleStrategy(lifecycleHandler);

        if (getCamelContext().isRunAllowed()) {
            lifecycleHandler.setInitialized(true);
        }

        super.doInit();
    }

    @Override
    protected void doStop() throws Exception {
        getCamelContext().getLifecycleStrategies().remove(lifecycleHandler);
        super.doStop();
    }

    void onEndpointAdd(KameletEndpoint endpoint) {
        lifecycleHandler.track(endpoint);
    }

    /*
     * This LifecycleHandler is used to keep track of created kamelet endpoints during startup as
     * we need to defer create routes from templates until camel context has finished loading
     * all routes and whatnot.
     *
     * Once the camel context is initialized all the endpoint tracked by this LifecycleHandler will
     * be used to create routes from templates.
     */
    private static class LifecycleHandler extends LifecycleStrategySupport {
        private final List<KameletEndpoint> endpoints;
        private final AtomicBoolean initialized;

        public LifecycleHandler() {
            this.endpoints = new ArrayList<>();
            this.initialized = new AtomicBoolean();
        }

        @Override
        public void onContextInitialized(CamelContext context) throws VetoCamelContextStartException {
            if (!this.initialized.compareAndExchange(false, true)) {
                for (KameletEndpoint endpoint : endpoints) {
                    try {
                        Kamelet.createRouteForEndpoint(endpoint);
                    } catch (Exception e) {
                        throw new VetoCamelContextStartException("Failure creating route from template: " + endpoint.getTemplateId(), e, context);
                    }
                }

                endpoints.clear();
            }
        }

        public void setInitialized(boolean initialized) {
            this.initialized.set(initialized);
        }

        public void track(KameletEndpoint endpoint) {
            if (this.initialized.get()) {
                try {
                    Kamelet.createRouteForEndpoint(endpoint);
                } catch (Exception e) {
                    throw RuntimeCamelException.wrapRuntimeException(e);
                }
            } else {
                this.endpoints.add(endpoint);
            }
        }
    }
}
