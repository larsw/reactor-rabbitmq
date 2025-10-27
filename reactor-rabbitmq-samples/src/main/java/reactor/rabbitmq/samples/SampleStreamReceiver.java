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
import com.rabbitmq.stream.OffsetSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.rabbitmq.StreamConsumeOptions;
import reactor.rabbitmq.StreamReceiver;
import reactor.rabbitmq.StreamReceiverOptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Sample demonstrating how to receive messages from a RabbitMQ stream.
 */
public class SampleStreamReceiver {

    private static final String STREAM = "demo-stream";
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleStreamReceiver.class);

    private final StreamReceiver streamReceiver;

    public SampleStreamReceiver() {
        Environment environment = Environment.builder().build();
        this.streamReceiver = new StreamReceiver(new StreamReceiverOptions().environment(environment));
    }

    public void receive(String stream, int count, CountDownLatch latch) {
        // Configure to start reading from the beginning of the stream
        StreamConsumeOptions options = new StreamConsumeOptions()
            .offset(OffsetSpecification.first())
            .name("sample-consumer");

        streamReceiver.consumeAutoAck(stream, options)
            .take(count)
            .subscribe(
                delivery -> {
                    LOGGER.info("Received message: {} at offset {}",
                        new String(delivery.getBody()),
                        delivery.getOffset());
                    latch.countDown();
                },
                error -> LOGGER.error("Receive error", error),
                () -> LOGGER.info("Reception complete")
            );
    }

    public void close() {
        this.streamReceiver.close();
    }

    public static void main(String[] args) throws Exception {
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);
        SampleStreamReceiver receiver = new SampleStreamReceiver();
        receiver.receive(STREAM, count, latch);
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        if (completed) {
            LOGGER.info("All messages received successfully");
        } else {
            LOGGER.warn("Not all messages were received");
        }
        receiver.close();
    }
}
