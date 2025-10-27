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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.OffsetSpecification;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RabbitMQ Streams support.
 * 
 * These tests require a running RabbitMQ server with streams enabled.
 * They will be skipped if the server is not available.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamsIntegrationTest {

    private static final String STREAM_NAME_PREFIX = "test-stream-";
    private Sender sender;
    private ConnectionFactory connectionFactory;
    private Environment streamEnvironment;

    @BeforeAll
    void setupAll() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        
        try {
            streamEnvironment = Environment.builder()
                .host("localhost")
                .port(5552)  // Default stream port
                .build();
        } catch (Exception e) {
            // Stream environment not available, tests will be skipped
            streamEnvironment = null;
        }
    }

    @BeforeEach
    void setup() {
        if (streamEnvironment == null) {
            return;  // Skip if environment not available
        }
        
        SenderOptions senderOptions = new SenderOptions()
            .connectionFactory(connectionFactory);
        sender = new Sender(senderOptions);
    }

    @AfterEach
    void tearDown() {
        if (sender != null) {
            sender.close();
        }
    }

    @AfterAll
    void tearDownAll() {
        if (streamEnvironment != null) {
            try {
                streamEnvironment.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    void shouldDeclareStreamQueue() {
        if (streamEnvironment == null) {
            System.out.println("Skipping test - RabbitMQ stream not available");
            return;
        }

        String streamName = STREAM_NAME_PREFIX + UUID.randomUUID();
        
        try {
            StreamSpecification spec = StreamSpecification.stream(streamName)
                .maxLengthBytes(10_000_000L)
                .maxSegmentSizeBytes(5_000_000);

            AMQP.Queue.DeclareOk result = sender.declare(spec)
                .block(Duration.ofSeconds(10));

            assertNotNull(result);
            assertEquals(streamName, result.getQueue());
        } finally {
            // Cleanup
            try {
                sender.delete(StreamSpecification.stream(streamName))
                    .block(Duration.ofSeconds(10));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    void shouldSendAndReceiveMessages() {
        if (streamEnvironment == null) {
            System.out.println("Skipping test - RabbitMQ stream not available");
            return;
        }

        String streamName = STREAM_NAME_PREFIX + UUID.randomUUID();
        
        try {
            // Declare stream
            StreamSpecification spec = StreamSpecification.stream(streamName)
                .maxLengthBytes(10_000_000L);
            
            sender.declare(spec)
                .block(Duration.ofSeconds(10));

            // Send messages using StreamSender
            try (StreamSender streamSender = new StreamSender(
                    new StreamSenderOptions().environment(streamEnvironment))) {
                
                int messageCount = 10;
                Flux<OutboundMessage> messages = Flux.range(0, messageCount)
                    .map(i -> new OutboundMessage("", "", ("Message " + i).getBytes()));

                long confirmedCount = streamSender.send(streamName, messages)
                    .filter(result -> result.isConfirmed())
                    .count()
                    .block(Duration.ofSeconds(30));

                assertEquals(messageCount, confirmedCount);
            }

            // Receive messages using StreamReceiver
            try (StreamReceiver streamReceiver = new StreamReceiver(
                    new StreamReceiverOptions().environment(streamEnvironment))) {
                
                CountDownLatch latch = new CountDownLatch(10);
                AtomicInteger receivedCount = new AtomicInteger(0);

                StreamConsumeOptions consumeOptions = new StreamConsumeOptions()
                    .offset(OffsetSpecification.first());

                streamReceiver.consumeAutoAck(streamName, consumeOptions)
                    .take(10)
                    .subscribe(
                        delivery -> {
                            receivedCount.incrementAndGet();
                            latch.countDown();
                        },
                        error -> fail("Error receiving messages: " + error.getMessage())
                    );

                assertTrue(latch.await(30, TimeUnit.SECONDS), 
                    "Should receive all messages within timeout");
                assertEquals(10, receivedCount.get());
            }
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage(), e);
        } finally {
            // Cleanup
            try {
                sender.delete(StreamSpecification.stream(streamName))
                    .block(Duration.ofSeconds(10));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    void streamSpecificationShouldHaveCorrectArguments() {
        StreamSpecification spec = StreamSpecification.stream("test")
            .maxLengthBytes(1_000_000L)
            .maxAge("7D")
            .maxSegmentSizeBytes(500_000);

        var args = spec.getArguments();
        
        assertThat(args).isNotNull();
        assertThat(args.get("x-queue-type")).isEqualTo("stream");
        assertThat(args.get("x-max-length-bytes")).isEqualTo(1_000_000L);
        assertThat(args.get("x-max-age")).isEqualTo("7D");
        assertThat(args.get("x-stream-max-segment-size-bytes")).isEqualTo(500_000);
    }
}
