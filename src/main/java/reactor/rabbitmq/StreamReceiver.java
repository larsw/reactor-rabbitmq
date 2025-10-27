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

import com.rabbitmq.stream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive abstraction to receive messages from RabbitMQ streams.
 * <p>
 * Streams are a special type of queue in RabbitMQ that are optimized for
 * high throughput and large message retention.
 */
public class StreamReceiver implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamReceiver.class);

    private final Environment environment;
    private final boolean privateEnvironment;
    private final Scheduler scheduler;
    private final boolean privateScheduler;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Create a stream receiver with default options.
     */
    public StreamReceiver() {
        this(new StreamReceiverOptions());
    }

    /**
     * Create a stream receiver with the given options.
     *
     * @param options the receiver options
     */
    public StreamReceiver(StreamReceiverOptions options) {
        if (options.getEnvironment() == null) {
            this.environment = Environment.builder().build();
            this.privateEnvironment = true;
        } else {
            this.environment = options.getEnvironment();
            this.privateEnvironment = false;
        }

        if (options.getScheduler() == null) {
            this.scheduler = Schedulers.newBoundedElastic(
                Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
                Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                "rabbitmq-stream-receiver"
            );
            this.privateScheduler = true;
        } else {
            this.scheduler = options.getScheduler();
            this.privateScheduler = false;
        }
    }

    /**
     * Consume messages from a stream.
     *
     * @param stream the name of the stream
     * @return a flux of stream deliveries
     */
    public Flux<StreamDelivery> consumeAutoAck(String stream) {
        return consumeAutoAck(stream, new StreamConsumeOptions());
    }

    /**
     * Consume messages from a stream with options.
     *
     * @param streamName the name of the stream
     * @param options    the consume options
     * @return a flux of stream deliveries
     */
    public Flux<StreamDelivery> consumeAutoAck(String streamName, StreamConsumeOptions options) {
        return Mono.<Consumer>create(sink -> {
            try {
                Sinks.Many<StreamDelivery> deliverySink = Sinks.many().multicast().onBackpressureBuffer();
                
                ConsumerBuilder builder = environment.consumerBuilder()
                    .stream(streamName);
                
                if (options.getOffset() != null) {
                    builder.offset(options.getOffset());
                }
                
                if (options.getName() != null) {
                    builder.name(options.getName());
                }
                
                if (options.getManualTrackingStrategy() != null) {
                    builder.manualTrackingStrategy();
                }
                
                builder.messageHandler((context, message) -> {
                    StreamDelivery delivery = new StreamDelivery(
                        message,
                        context.offset(),
                        context.timestamp(),
                        streamName
                    );
                    deliverySink.tryEmitNext(delivery);
                });
                
                Consumer consumer = builder.build();
                
                // Store consumer for cleanup
                sink.success(consumer);
            } catch (Exception e) {
                sink.error(new RabbitFluxException("Error creating stream consumer", e));
            }
        })
        .subscribeOn(scheduler)
        .flatMapMany(consumer -> {
            // Get the sink that was created during consumer setup
            Sinks.Many<StreamDelivery> deliverySink = Sinks.many().multicast().onBackpressureBuffer();
            
            // Recreate consumer with proper message handler
            try {
                consumer.close();
                
                ConsumerBuilder builder = environment.consumerBuilder()
                    .stream(streamName);
                
                if (options.getOffset() != null) {
                    builder.offset(options.getOffset());
                }
                
                if (options.getName() != null) {
                    builder.name(options.getName());
                }
                
                if (options.getManualTrackingStrategy() != null) {
                    builder.manualTrackingStrategy();
                }
                
                builder.messageHandler((context, message) -> {
                    StreamDelivery delivery = new StreamDelivery(
                        message,
                        context.offset(),
                        context.timestamp(),
                        streamName
                    );
                    deliverySink.tryEmitNext(delivery);
                });
                
                Consumer newConsumer = builder.build();
                
                return deliverySink.asFlux()
                    .doFinally(signalType -> {
                        try {
                            newConsumer.close();
                        } catch (Exception e) {
                            LOGGER.warn("Error closing stream consumer", e);
                        }
                    });
            } catch (Exception e) {
                return Flux.error(new RabbitFluxException("Error creating stream consumer", e));
            }
        });
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (privateScheduler) {
                scheduler.dispose();
            }
            if (privateEnvironment) {
                try {
                    environment.close();
                } catch (Exception e) {
                    LOGGER.warn("Error closing stream environment", e);
                }
            }
        }
    }

    /**
     * A delivery from a stream.
     */
    public static class StreamDelivery {
        private final Message message;
        private final long offset;
        private final long timestamp;
        private final String stream;

        public StreamDelivery(Message message, long offset, long timestamp, String stream) {
            this.message = message;
            this.offset = offset;
            this.timestamp = timestamp;
            this.stream = stream;
        }

        public Message getMessage() {
            return message;
        }

        public byte[] getBody() {
            return message.getBodyAsBinary();
        }

        public long getOffset() {
            return offset;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getStream() {
            return stream;
        }
    }
}
