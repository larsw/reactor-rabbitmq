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

package reactor.rabbitmq.samples;

import com.rabbitmq.stream.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Sample demonstrating how to send messages to a RabbitMQ stream.
 */
public class SampleStreamSender {

    private static final String STREAM = "demo-stream";
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleStreamSender.class);

    private final Sender sender;
    private final StreamSender streamSender;
    private final Environment environment;

    public SampleStreamSender() {
        // Create a regular sender for stream declaration (using AMQP)
        this.sender = RabbitFlux.createSender();
        
        // Create stream environment for publishing (using Stream protocol)
        this.environment = Environment.builder().build();
        this.streamSender = new StreamSender(new StreamSenderOptions().environment(environment));
    }

    public void send(String stream, int count, CountDownLatch latch) {
        // First, declare the stream using AMQP
        StreamSpecification streamSpec = StreamSpecification.stream(stream)
            .maxLengthBytes(10_000_000L)  // 10 MB max size
            .maxSegmentSizeBytes(5_000_000);  // 5 MB segments

        Flux<OutboundMessage> messages = Flux.range(1, count)
            .map(i -> new OutboundMessage("", stream, ("Stream_Message_" + i).getBytes()));

        sender.declare(streamSpec)
            .thenMany(streamSender.send(stream, messages))
                .doOnError(e -> LOGGER.error("Send failed", e))
                .subscribe(result -> {
                    if (result.isConfirmed()) {
                        LOGGER.info("Message sent successfully to stream");
                        latch.countDown();
                    } else {
                        LOGGER.error("Message not confirmed", result.getError());
                    }
                });
    }

    public void close() {
        this.streamSender.close();
        this.sender.close();
    }

    public static void main(String[] args) throws Exception {
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);
        SampleStreamSender sender = new SampleStreamSender();
        sender.send(STREAM, count, latch);
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        if (completed) {
            LOGGER.info("All messages sent successfully");
        } else {
            LOGGER.warn("Not all messages were confirmed");
        }
        sender.close();
    }
}
