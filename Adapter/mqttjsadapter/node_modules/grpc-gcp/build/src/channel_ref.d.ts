/**
 * @license
 * Copyright 2018 gRPC authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
import { Channel } from 'grpc';
/**
 * A wrapper of real grpc channel. Also provides helper functions to
 * calculate affinity counts and active streams count.
 */
export declare class ChannelRef {
    private readonly channel;
    private readonly channelId;
    private affinityCount;
    private activeStreamsCount;
    /**
     * @param channel The underlying grpc channel.
     * @param channelId Id for creating unique channel.
     * @param affinityCount Initial affinity count.
     * @param activeStreamsCount Initial streams count.
     */
    constructor(channel: Channel, channelId: number, affinityCount?: number, activeStreamsCount?: number);
    affinityCountIncr(): void;
    activeStreamsCountIncr(): void;
    affinityCountDecr(): void;
    activeStreamsCountDecr(): void;
    getAffinityCount(): number;
    getActiveStreamsCount(): number;
    getChannel(): Channel;
}
