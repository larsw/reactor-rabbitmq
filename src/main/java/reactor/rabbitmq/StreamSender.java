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
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive abstraction to send messages to RabbitMQ streams.
 * <p>
 * Streams are a special type of queue in RabbitMQ that are optimized for
 * high throughput and large message retention.
 */
public class StreamSender implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamSender.class);

    private final Environment environment;
    private final boolean privateEnvironment;
    private final Scheduler scheduler;
    private final boolean privateScheduler;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Create a stream sender with default options.
     */
    public StreamSender() {
        this(new StreamSenderOptions());
    }

    /**
     * Create a stream sender with the given options.
     *
     * @param options the sender options
     */
    public StreamSender(StreamSenderOptions options) {
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
                "rabbitmq-stream-sender"
            );
            this.privateScheduler = true;
        } else {
            this.scheduler = options.getScheduler();
            this.privateScheduler = false;
        }
    }

    /**
     * Send messages to a stream.
     *
     * @param stream   the name of the stream
     * @param messages the flux of messages to send
     * @return a flux of confirmation results
     */
    public Flux<StreamSendResult> send(String stream, Publisher<OutboundMessage> messages) {
        return send(stream, messages, new StreamSendOptions());
    }

    /**
     * Send messages to a stream with options.
     *
     * @param streamName the name of the stream
     * @param messages   the flux of messages to send
     * @param options    the send options
     * @return a flux of confirmation results
     */
    public Flux<StreamSendResult> send(String streamName, Publisher<OutboundMessage> messages, StreamSendOptions options) {
        return Mono.<Producer>create(sink -> {
            try {
                ProducerBuilder builder = environment.producerBuilder()
                    .stream(streamName);
                
                if (options.getMaxUnconfirmedMessages() != null) {
                    builder.maxUnconfirmedMessages(options.getMaxUnconfirmedMessages());
                }
                
                if (options.getBatchSize() != null) {
                    builder.batchSize(options.getBatchSize());
                }
                
                if (options.getSubEntrySize() != null) {
                    builder.subEntrySize(options.getSubEntrySize());
                }
                
                Producer producer = builder.build();
                sink.success(producer);
            } catch (Exception e) {
                sink.error(new RabbitFluxException("Error creating stream producer", e));
            }
        })
        .subscribeOn(scheduler)
        .flatMapMany(producer -> 
            Flux.from(messages)
                .flatMap(message -> {
                    return Mono.<StreamSendResult>create(sink -> {
                        try {
                            Message streamMessage = producer.messageBuilder()
                                .addData(message.getBody())
                                .properties()
                                    .messageId(message.getProperties() != null ? message.getProperties().getMessageId() : null)
                                    .correlationId(message.getProperties() != null ? message.getProperties().getCorrelationId() : null)
                                    .contentType(message.getProperties() != null ? message.getProperties().getContentType() : null)
                                    .contentEncoding(message.getProperties() != null ? message.getProperties().getContentEncoding() : null)
                                    .messageBuilder()
                                .build();
                            
                            ConfirmationHandler confirmationHandler = confirmationStatus -> {
                                if (confirmationStatus.isConfirmed()) {
                                    sink.success(new StreamSendResult(message, true, null));
                                } else {
                                    sink.success(new StreamSendResult(message, false, 
                                        new RabbitFluxException("Message not confirmed: " + confirmationStatus.getCode())));
                                }
                            };
                            
                            producer.send(streamMessage, confirmationHandler);
                        } catch (Exception e) {
                            sink.error(new RabbitFluxException("Error sending message to stream", e));
                        }
                    });
                })
                .doFinally(signalType -> {
                    try {
                        producer.close();
                    } catch (Exception e) {
                        LOGGER.warn("Error closing stream producer", e);
                    }
                })
        );
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
     * Result of sending a message to a stream.
     */
    public static class StreamSendResult {
        private final OutboundMessage message;
        private final boolean confirmed;
        private final Exception error;

        public StreamSendResult(OutboundMessage message, boolean confirmed, @Nullable Exception error) {
            this.message = message;
            this.confirmed = confirmed;
            this.error = error;
        }

        public OutboundMessage getMessage() {
            return message;
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        @Nullable
        public Exception getError() {
            return error;
        }
    }
}
