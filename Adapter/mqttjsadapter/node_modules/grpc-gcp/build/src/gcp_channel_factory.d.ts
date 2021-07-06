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
import * as grpc from 'grpc';
import { ChannelRef } from './channel_ref';
import * as protoRoot from './generated/grpc_gcp';
import IAffinityConfig = protoRoot.grpc.gcp.IAffinityConfig;
/**
 * A channel management factory that implements grpc.Channel APIs.
 */
export declare class GcpChannelFactory {
    private maxSize;
    private maxConcurrentStreamsLowWatermark;
    private options;
    private methodToAffinity;
    private affinityKeyToChannelRef;
    private channelRefs;
    private target;
    private credentials;
    /**
     * @param address The address of the server to connect to.
     * @param credentials Channel credentials to use when connecting
     * @param options A map of channel options.
     */
    constructor(address: string, credentials: grpc.ChannelCredentials, options: any);
    private initMethodToAffinityMap;
    /**
     * Picks a grpc channel from the pool and wraps it with ChannelRef.
     * @param affinityKey Affinity key to get the bound channel.
     * @return Wrapper containing the grpc channel.
     */
    getChannelRef(affinityKey?: string): ChannelRef;
    /**
     * Get AffinityConfig associated with a certain method.
     * @param methodName Method name of the request.
     */
    getAffinityConfig(methodName: string): IAffinityConfig;
    /**
     * Bind channel with affinity key.
     * @param channelRef ChannelRef instance that contains the grpc channel.
     * @param affinityKey The affinity key used for binding the channel.
     */
    bind(channelRef: ChannelRef, affinityKey: string): void;
    /**
     * Unbind channel with affinity key.
     * @param boundKey Affinity key bound to a channel.
     */
    unbind(boundKey?: string): void;
    /**
     * Close all channels in the channel pool.
     */
    close(): void;
    getTarget(): string;
    /**
     * Get the current connectivity state of the channel pool.
     * @param tryToConnect If true, the channel will start connecting if it is
     *     idle. Otherwise, idle channels will only start connecting when a
     *     call starts.
     * @return connectivity state of channel pool.
     */
    getConnectivityState(tryToConnect: boolean): grpc.connectivityState;
    /**
     * Watch for connectivity state changes. Currently This function will throw
     * not implemented error because the implementation requires lot of work but
     * has little use cases.
     * @param currentState The state to watch for transitions from. This should
     *     always be populated by calling getConnectivityState immediately before.
     * @param deadline A deadline for waiting for a state change
     * @param callback Called with no error when the state changes, or with an
     *     error if the deadline passes without a state change
     */
    watchConnectivityState(currentState: grpc.connectivityState, deadline: grpc.Deadline, callback: Function): void;
    /**
     * Create a call object. This function will not be called when using
     * grpc.Client class. But since it's a public function of grpc.Channel,
     * It needs to be implemented for potential use cases.
     * @param method The full method string to request.
     * @param deadline The call deadline.
     * @param host A host string override for making the request.
     * @param parentCall A server call to propagate some information from.
     * @param propagateFlags A bitwise combination of elements of
     *     {@link grpc.propagate} that indicates what information to propagate
     *     from parentCall.
     * @return a grpc call object.
     */
    createCall(method: string, deadline: grpc.Deadline, host: string | null, parentCall: grpc.Call | null, propagateFlags: number | null): grpc.Call;
}
