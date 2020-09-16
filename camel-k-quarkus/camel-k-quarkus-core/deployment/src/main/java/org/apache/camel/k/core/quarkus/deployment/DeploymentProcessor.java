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
package org.apache.camel.k.core.quarkus.deployment;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.apache.camel.k.Constants;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.SourceDefinition;
import org.apache.camel.k.SourceLoader;
import org.apache.camel.k.core.quarkus.RuntimeRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.spi.StreamCachingStrategy;
import org.jboss.jandex.IndexView;

import static org.apache.camel.k.core.quarkus.deployment.DeploymentSupport.getAllKnownImplementors;
import static org.apache.camel.k.core.quarkus.deployment.DeploymentSupport.reflectiveClassBuildItem;
import static org.apache.camel.k.core.quarkus.deployment.DeploymentSupport.stream;

public class DeploymentProcessor {
    @BuildStep
    List<CamelServicePatternBuildItem> servicePatterns() {
        return List.of(
            new CamelServicePatternBuildItem(
                CamelServiceDestination.REGISTRY,
                true,
                Constants.SOURCE_LOADER_RESOURCE_PATH + "/*",
                Constants.CONTEXT_CUSTOMIZER_RESOURCE_PATH  + "/*"),
            new CamelServicePatternBuildItem(
                CamelServiceDestination.DISCOVERY,
                true,
                Constants.SOURCE_LOADER_INTERCEPTOR_RESOURCE_PATH + "/*")
        );
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerClasses(CombinedIndexBuildItem index) {
        return List.of(
            reflectiveClassBuildItem(SourceDefinition.class),
            reflectiveClassBuildItem(getAllKnownImplementors(index.getIndex(), ContextCustomizer.class)),
            reflectiveClassBuildItem(getAllKnownImplementors(index.getIndex(), SourceLoader.Interceptor.class))
       );
    }

    @BuildStep
    List<ServiceProviderBuildItem> registerServices(CombinedIndexBuildItem combinedIndexBuildItem) {
        final IndexView view = combinedIndexBuildItem.getIndex();
        final String serviceType = "org.apache.camel.k.Runtime$Listener";

        return stream(getAllKnownImplementors(view, serviceType))
            .map(i -> new ServiceProviderBuildItem(serviceType, i.name().toString()))
            .collect(Collectors.toList());
    }

    @BuildStep
    void registerStreamCachingClasses(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {

        final IndexView view = combinedIndex.getIndex();

        getAllKnownImplementors(view, StreamCachingStrategy.class)
            .forEach(i-> reflectiveClass.produce(reflectiveClassBuildItem(i)));

        getAllKnownImplementors(view, StreamCachingStrategy.Statistics.class)
            .forEach(i-> reflectiveClass.produce(reflectiveClassBuildItem(i)));

        getAllKnownImplementors(view, StreamCachingStrategy.SpoolRule.class)
            .forEach(i-> reflectiveClass.produce(reflectiveClassBuildItem(i)));

        reflectiveClass.produce(
            new ReflectiveClassBuildItem(
                true,
                false,
                StreamCachingStrategy.SpoolRule.class)
        );
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void customizeContext(RuntimeRecorder recorder, BuildProducer<CamelContextCustomizerBuildItem> customizers) {
        customizers.produce(new CamelContextCustomizerBuildItem(recorder.registerCompositeClassLoader()));
    }
}
