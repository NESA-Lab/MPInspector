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
import { GcpChannelFactory } from './gcp_channel_factory';
import * as protoRoot from './generated/grpc_gcp';
import ApiConfig = protoRoot.grpc.gcp.ApiConfig;
/**
 * Create ApiConfig proto message from config object.
 * @param apiDefinition Api object that specifies channel pool configuation.
 * @return A protobuf message type.
 */
export declare function createGcpApiConfig(apiDefinition: {}): ApiConfig;
/**
 * Function for creating a gcp channel factory.
 * @memberof grpc-gcp
 * @param address The address of the server to connect to.
 * @param credentials Channel credentials to use when connecting
 * @param options A map of channel options that will be passed to the core.
 * @return {GcpChannelFactory} A GcpChannelFactory instance.
 */
export declare function gcpChannelFactoryOverride(address: string, credentials: grpc.ChannelCredentials, options: {}): GcpChannelFactory;
export interface MethodDefinition<RequestType, ResponseType> {
    path: string;
    requestStream: boolean;
    responseStream: boolean;
    requestSerialize: grpc.serialize<RequestType>;
    responseDeserialize: grpc.deserialize<ResponseType>;
}
export interface InputCallProperties<RequestType, ResponseType> {
    argument?: any;
    metadata: grpc.Metadata;
    call: grpc.ClientUnaryCall | grpc.ClientReadableStream<RequestType> | grpc.ClientDuplexStream<RequestType, ResponseType> | grpc.ClientWritableStream<RequestType>;
    channel: GcpChannelFactory;
    methodDefinition: MethodDefinition<RequestType, ResponseType>;
    callOptions: grpc.CallOptions;
    callback?: Function;
}
export interface OutputCallProperties<RequestType, ResponseType> {
    argument?: any;
    metadata: grpc.Metadata;
    call: grpc.ClientUnaryCall | grpc.ClientReadableStream<RequestType> | grpc.ClientDuplexStream<RequestType, ResponseType> | grpc.ClientWritableStream<RequestType>;
    channel: grpc.Channel;
    methodDefinition: MethodDefinition<RequestType, ResponseType>;
    callOptions: grpc.CallOptions;
    callback?: Function;
}
/**
 * Pass in call properties and return a new object with modified values.
 * This function will be used together with gcpChannelFactoryOverride
 * when constructing a grpc Client.
 * @memberof grpc-gcp
 * @param callProperties Call properties with channel factory object.
 * @return Modified call properties with selected grpc channel object.
 */
export declare function gcpCallInvocationTransformer<RequestType, ResponseType>(callProperties: InputCallProperties<RequestType, ResponseType>): OutputCallProperties<RequestType, ResponseType>;
export { GcpChannelFactory };
