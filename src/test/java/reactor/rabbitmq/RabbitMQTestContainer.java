/*
 * Copyright (c) 2024 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.rabbitmq;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * JUnit 5 extension that manages a shared RabbitMQ Testcontainer instance.
 * The container is started once before all tests and reused across test classes.
 */
public class RabbitMQTestContainer implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static RabbitMQContainer rabbitMQContainer;
    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            synchronized (RabbitMQTestContainer.class) {
                if (!started) {
                    // Use management image which includes rabbitmqctl and all management tools
                    rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management"));
                    
                    // Use setPortBindings to bind to fixed port 5672
                    rabbitMQContainer.setPortBindings(java.util.Collections.singletonList("5672:5672"));
                    
                    rabbitMQContainer.start();
                    
                    // Set system property for rabbitmqctl to use the container
                    // Use the actual container ID which Docker exec accepts
                    String containerId = rabbitMQContainer.getContainerId();
                    System.setProperty("rabbitmqctl.bin", "DOCKER:" + containerId);
                    
                    started = true;
                    
                    // Register shutdown hook
                    context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
                            .put("RabbitMQTestContainer", this);
                }
            }
        }
    }

    @Override
    public void close() {
        if (rabbitMQContainer != null) {
            rabbitMQContainer.stop();
        }
    }

    public static RabbitMQContainer getContainer() {
        return rabbitMQContainer;
    }
}
