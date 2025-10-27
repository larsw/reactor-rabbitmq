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

import com.rabbitmq.stream.OffsetSpecification;
import reactor.util.annotation.Nullable;

/**
 * Options for consuming messages from a stream.
 */
public class StreamConsumeOptions {

    private OffsetSpecification offset;
    private String name;
    private Boolean manualTrackingStrategy;

    /**
     * Set the offset from which to start consuming.
     * <p>
     * Common offset specifications:
     * <ul>
     *   <li>{@link OffsetSpecification#first()} - start from the beginning</li>
     *   <li>{@link OffsetSpecification#last()} - start from the end</li>
     *   <li>{@link OffsetSpecification#next()} - start from next message</li>
     *   <li>{@link OffsetSpecification#offset(long)} - start from a specific offset</li>
     * </ul>
     *
     * @param offset the offset specification
     * @return this options instance
     */
    public StreamConsumeOptions offset(OffsetSpecification offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Set the name of the consumer.
     * <p>
     * Consumer names are useful for tracking and offset management.
     *
     * @param name the consumer name
     * @return this options instance
     */
    public StreamConsumeOptions name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Enable manual tracking strategy.
     * <p>
     * When enabled, the application is responsible for storing offset tracking.
     *
     * @return this options instance
     */
    public StreamConsumeOptions manualTrackingStrategy() {
        this.manualTrackingStrategy = true;
        return this;
    }

    @Nullable
    public OffsetSpecification getOffset() {
        return offset;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Boolean getManualTrackingStrategy() {
        return manualTrackingStrategy;
    }
}
