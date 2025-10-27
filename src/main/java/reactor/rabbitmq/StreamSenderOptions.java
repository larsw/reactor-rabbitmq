/*
 * Copyright (c) 2025 VMware Inc. or its affiliates, All Rights Reserved.
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

import com.rabbitmq.stream.Environment;
import reactor.core.scheduler.Scheduler;
import reactor.util.annotation.Nullable;

/**
 * Options for configuring a {@link StreamSender}.
 */
public class StreamSenderOptions {

    private Environment environment;
    private Scheduler scheduler;

    /**
     * Set the stream environment to use.
     * <p>
     * If not set, a default environment will be created.
     *
     * @param environment the stream environment
     * @return this options instance
     */
    public StreamSenderOptions environment(Environment environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Set the scheduler to use for stream operations.
     * <p>
     * If not set, a default bounded elastic scheduler will be created.
     *
     * @param scheduler the scheduler
     * @return this options instance
     */
    public StreamSenderOptions scheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    @Nullable
    public Environment getEnvironment() {
        return environment;
    }

    @Nullable
    public Scheduler getScheduler() {
        return scheduler;
    }
}
