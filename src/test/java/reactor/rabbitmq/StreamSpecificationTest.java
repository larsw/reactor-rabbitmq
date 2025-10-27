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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StreamSpecificationTest {

    @Test
    void streamShouldBeAlwaysDurable() {
        StreamSpecification spec = StreamSpecification.stream("my-stream");
        assertTrue(spec.isDurable());
    }

    @Test
    void streamShouldNeverBeExclusive() {
        StreamSpecification spec = StreamSpecification.stream("my-stream");
        assertFalse(spec.isExclusive());
    }

    @Test
    void streamShouldNeverBeAutoDelete() {
        StreamSpecification spec = StreamSpecification.stream("my-stream");
        assertFalse(spec.isAutoDelete());
    }

    @Test
    void streamShouldNotAllowSettingDurableToFalse() {
        StreamSpecification spec = StreamSpecification.stream("my-stream");
        assertThrows(
            IllegalArgumentException.class,
            () -> spec.durable(false),
            "Streams are always durable"
        );
    }

    @Test
    void streamShouldNotAllowSettingExclusiveToTrue() {
        StreamSpecification spec = StreamSpecification.stream("my-stream");
        assertThrows(
            IllegalArgumentException.class,
            () -> spec.exclusive(true),
            "Streams cannot be exclusive"
        );
    }

    @Test
    void streamShouldNotAllowSettingAutoDeleteToTrue() {
        StreamSpecification spec = StreamSpecification.stream("my-stream");
        assertThrows(
            IllegalArgumentException.class,
            () -> spec.autoDelete(true),
            "Streams cannot be auto-delete"
        );
    }

    @Test
    void streamShouldIncludeQueueTypeInArguments() {
        StreamSpecification spec = StreamSpecification.stream("my-stream");
        Map<String, Object> args = spec.getArguments();
        
        assertThat(args).isNotNull();
        assertThat(args.get("x-queue-type")).isEqualTo("stream");
    }

    @Test
    void streamShouldSupportMaxLengthBytes() {
        StreamSpecification spec = StreamSpecification.stream("my-stream")
            .maxLengthBytes(1000000L);
        
        Map<String, Object> args = spec.getArguments();
        assertThat(args.get("x-max-length-bytes")).isEqualTo(1000000L);
    }

    @Test
    void streamShouldSupportMaxAge() {
        StreamSpecification spec = StreamSpecification.stream("my-stream")
            .maxAge("7D");
        
        Map<String, Object> args = spec.getArguments();
        assertThat(args.get("x-max-age")).isEqualTo("7D");
    }

    @Test
    void streamShouldSupportMaxSegmentSizeBytes() {
        StreamSpecification spec = StreamSpecification.stream("my-stream")
            .maxSegmentSizeBytes(500000000);
        
        Map<String, Object> args = spec.getArguments();
        assertThat(args.get("x-stream-max-segment-size-bytes")).isEqualTo(500000000);
    }

    @Test
    void streamShouldSupportAllOptionsTogeth() {
        StreamSpecification spec = StreamSpecification.stream("my-stream")
            .maxLengthBytes(2000000000L)
            .maxAge("30D")
            .maxSegmentSizeBytes(500000000)
            .passive(false);
        
        assertEquals("my-stream", spec.getName());
        assertTrue(spec.isDurable());
        assertFalse(spec.isExclusive());
        assertFalse(spec.isAutoDelete());
        assertFalse(spec.isPassive());
        
        Map<String, Object> args = spec.getArguments();
        assertThat(args.get("x-queue-type")).isEqualTo("stream");
        assertThat(args.get("x-max-length-bytes")).isEqualTo(2000000000L);
        assertThat(args.get("x-max-age")).isEqualTo("30D");
        assertThat(args.get("x-stream-max-segment-size-bytes")).isEqualTo(500000000);
    }

    @Test
    void streamShouldReturnCorrectProperties() {
        StreamSpecification spec = StreamSpecification.stream("test-stream")
            .maxLengthBytes(5000000L)
            .maxSegmentSizeBytes(100000000);
        
        assertEquals("test-stream", spec.getName());
        assertEquals(5000000L, spec.getMaxLengthBytes());
        assertEquals(100000000, spec.getMaxSegmentSizeBytes());
    }
}
