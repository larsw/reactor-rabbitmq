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

/**
 * Options for sending messages to a stream.
 */
public class StreamSendOptions {

    private Integer maxUnconfirmedMessages;
    private Integer batchSize;
    private Integer subEntrySize;

    /**
     * Set the maximum number of unconfirmed messages.
     * <p>
     * The producer will stop accepting messages when this limit is reached
     * until confirmations are received.
     *
     * @param maxUnconfirmedMessages the maximum number of unconfirmed messages
     * @return this options instance
     */
    public StreamSendOptions maxUnconfirmedMessages(int maxUnconfirmedMessages) {
        this.maxUnconfirmedMessages = maxUnconfirmedMessages;
        return this;
    }

    /**
     * Set the batch size for message publishing.
     * <p>
     * Messages will be batched together for better throughput.
     *
     * @param batchSize the batch size
     * @return this options instance
     */
    public StreamSendOptions batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Set the sub-entry size for message batching.
     * <p>
     * This allows for sub-entry batching which can improve throughput.
     *
     * @param subEntrySize the sub-entry size
     * @return this options instance
     */
    public StreamSendOptions subEntrySize(int subEntrySize) {
        this.subEntrySize = subEntrySize;
        return this;
    }

    @Nullable
    public Integer getMaxUnconfirmedMessages() {
        return maxUnconfirmedMessages;
    }

    @Nullable
    public Integer getBatchSize() {
        return batchSize;
    }

    @Nullable
    public Integer getSubEntrySize() {
        return subEntrySize;
    }
}
