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

import reactor.util.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Fluent API to specify the creation of a stream queue.
 * <p>
 * Stream queues are a special type of queue in RabbitMQ that are optimized for
 * high throughput and large message retention. Streams are always durable and
 * non-exclusive.
 * <p>
 * This class extends {@link QueueSpecification} and adds stream-specific
 * configuration options.
 */
public class StreamSpecification extends QueueSpecification {

    private Long maxLengthBytes;
    private Long maxAge;
    private Integer maxSegmentSizeBytes;
    
    /**
     * Create a new stream specification with the given name.
     *
     * @param name the name of the stream
     * @return a new stream specification
     */
    public static StreamSpecification stream(String name) {
        return new StreamSpecification(name);
    }

    private StreamSpecification(String name) {
        this.name = name;
        this.durable = true;  // streams are always durable
        this.exclusive = false;  // streams are never exclusive
        this.autoDelete = false;  // streams are never auto-delete
    }

    /**
     * Set the maximum size of the stream in bytes.
     * <p>
     * Once this limit is reached, older messages will be removed to make room for new ones.
     *
     * @param maxLengthBytes the maximum size in bytes
     * @return this stream specification
     */
    public StreamSpecification maxLengthBytes(long maxLengthBytes) {
        this.maxLengthBytes = maxLengthBytes;
        return this;
    }

    /**
     * Set the maximum age of messages in the stream.
     * <p>
     * Messages older than this will be removed. The value should be a string
     * like "7D" for 7 days, "12h" for 12 hours, etc.
     *
     * @param maxAge the maximum age string (e.g., "7D", "12h", "30m")
     * @return this stream specification
     */
    public StreamSpecification maxAge(String maxAge) {
        if (maxAge != null && !maxAge.isEmpty()) {
            // Store as-is, will be used in arguments map
            if (this.arguments == null) {
                this.arguments = new HashMap<>();
            }
            this.arguments.put("x-max-age", maxAge);
        }
        return this;
    }

    /**
     * Set the maximum segment size for the stream.
     * <p>
     * Streams are divided into segment files on disk. This sets the maximum
     * size of each segment file.
     *
     * @param maxSegmentSizeBytes the maximum segment size in bytes
     * @return this stream specification
     */
    public StreamSpecification maxSegmentSizeBytes(int maxSegmentSizeBytes) {
        this.maxSegmentSizeBytes = maxSegmentSizeBytes;
        return this;
    }

    /**
     * Streams are always durable. This method is overridden to prevent changing this.
     *
     * @throws IllegalArgumentException if durable is set to false
     */
    @Override
    public StreamSpecification durable(boolean durable) {
        if (!durable) {
            throw new IllegalArgumentException("Streams are always durable");
        }
        return this;
    }

    /**
     * Streams are never exclusive. This method is overridden to prevent changing this.
     *
     * @throws IllegalArgumentException if exclusive is set to true
     */
    @Override
    public StreamSpecification exclusive(boolean exclusive) {
        if (exclusive) {
            throw new IllegalArgumentException("Streams cannot be exclusive");
        }
        return this;
    }

    /**
     * Streams are never auto-delete. This method is overridden to prevent changing this.
     *
     * @throws IllegalArgumentException if autoDelete is set to true
     */
    @Override
    public StreamSpecification autoDelete(boolean autoDelete) {
        if (autoDelete) {
            throw new IllegalArgumentException("Streams cannot be auto-delete");
        }
        return this;
    }

    @Override
    public StreamSpecification name(@Nullable String name) {
        this.name = name;
        return this;
    }

    @Override
    public StreamSpecification arguments(@Nullable Map<String, Object> arguments) {
        this.arguments = arguments;
        return this;
    }

    @Override
    public StreamSpecification passive(boolean passive) {
        this.passive = passive;
        return this;
    }

    /**
     * Build the arguments map for stream creation.
     * This includes the stream-specific parameters.
     *
     * @return the arguments map
     */
    @Override
    @Nullable
    public Map<String, Object> getArguments() {
        Map<String, Object> args = this.arguments != null ? new HashMap<>(this.arguments) : new HashMap<>();
        
        // Set the queue type to stream
        args.put("x-queue-type", "stream");
        
        // Add stream-specific arguments if set
        if (maxLengthBytes != null) {
            args.put("x-max-length-bytes", maxLengthBytes);
        }
        
        if (maxSegmentSizeBytes != null) {
            args.put("x-stream-max-segment-size-bytes", maxSegmentSizeBytes);
        }
        
        return args;
    }

    @Nullable
    public Long getMaxLengthBytes() {
        return maxLengthBytes;
    }

    @Nullable
    public Integer getMaxSegmentSizeBytes() {
        return maxSegmentSizeBytes;
    }
}
