"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
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
const grpc = require("grpc");
const util = require("util");
const gcp_channel_factory_1 = require("./gcp_channel_factory");
exports.GcpChannelFactory = gcp_channel_factory_1.GcpChannelFactory;
const protoRoot = require("./generated/grpc_gcp");
var ApiConfig = protoRoot.grpc.gcp.ApiConfig;
var AffinityConfig = protoRoot.grpc.gcp.AffinityConfig;
/**
 * Create ApiConfig proto message from config object.
 * @param apiDefinition Api object that specifies channel pool configuation.
 * @return A protobuf message type.
 */
function createGcpApiConfig(apiDefinition) {
    return ApiConfig.fromObject(apiDefinition);
}
exports.createGcpApiConfig = createGcpApiConfig;
/**
 * Function for creating a gcp channel factory.
 * @memberof grpc-gcp
 * @param address The address of the server to connect to.
 * @param credentials Channel credentials to use when connecting
 * @param options A map of channel options that will be passed to the core.
 * @return {GcpChannelFactory} A GcpChannelFactory instance.
 */
function gcpChannelFactoryOverride(address, credentials, options) {
    return new gcp_channel_factory_1.GcpChannelFactory(address, credentials, options);
}
exports.gcpChannelFactoryOverride = gcpChannelFactoryOverride;
/**
 * Pass in call properties and return a new object with modified values.
 * This function will be used together with gcpChannelFactoryOverride
 * when constructing a grpc Client.
 * @memberof grpc-gcp
 * @param callProperties Call properties with channel factory object.
 * @return Modified call properties with selected grpc channel object.
 */
function gcpCallInvocationTransformer(callProperties) {
    const channelFactory = callProperties.channel;
    if (!channelFactory || !(channelFactory instanceof gcp_channel_factory_1.GcpChannelFactory)) {
        // The gcpCallInvocationTransformer needs to use gcp channel factory.
        return callProperties;
    }
    const argument = callProperties.argument;
    const metadata = callProperties.metadata;
    const call = callProperties.call;
    const methodDefinition = callProperties.methodDefinition;
    const path = methodDefinition.path;
    const callOptions = callProperties.callOptions;
    const callback = callProperties.callback;
    const preProcessResult = preProcess(channelFactory, path, argument);
    const channelRef = preProcessResult.channelRef;
    const boundKey = preProcessResult.boundKey;
    const postProcessInterceptor = (
    // tslint:disable-next-line:no-any options can be any object
    options, nextCall) => {
        // tslint:disable-next-line:no-any protobuf message
        let firstMessage;
        const requester = {
            start: (metadata, listener, next) => {
                const newListener = {
                    onReceiveMetadata: (metadata, next) => {
                        next(metadata);
                    },
                    // tslint:disable-next-line:no-any protobuf message
                    onReceiveMessage: (message, next) => {
                        if (!firstMessage)
                            firstMessage = message;
                        next(message);
                    },
                    onReceiveStatus: (status, next) => {
                        if (status.code === grpc.status.OK) {
                            postProcess(channelFactory, channelRef, path, boundKey, firstMessage);
                        }
                        next(status);
                    },
                };
                next(metadata, newListener);
            },
            // tslint:disable-next-line:no-any protobuf message
            sendMessage: (message, next) => {
                next(message);
            },
            halfClose: (next) => {
                next();
            },
            cancel: (next) => {
                next();
            },
        };
        return new grpc.InterceptingCall(nextCall(options), requester);
    };
    // Append interceptor to existing interceptors list.
    const newCallOptions = Object.assign({}, callOptions);
    const interceptors = callOptions.interceptors ? callOptions.interceptors : [];
    newCallOptions.interceptors = interceptors.concat([postProcessInterceptor]);
    return {
        argument,
        metadata,
        call,
        channel: channelRef.getChannel(),
        methodDefinition,
        callOptions: newCallOptions,
        callback,
    };
}
exports.gcpCallInvocationTransformer = gcpCallInvocationTransformer;
/**
 * Handle channel affinity and pick a channel before call starts.
 * @param channelFactory The channel management factory.
 * @param path Method path.
 * @param argument The request arguments object.
 * @return Result containing bound affinity key and the chosen channel ref
 * object.
 */
function preProcess(channelFactory, path, 
// tslint:disable-next-line:no-any protobuf message
argument) {
    const affinityConfig = channelFactory.getAffinityConfig(path);
    let boundKey;
    if (argument && affinityConfig) {
        const command = affinityConfig.command;
        if (command === AffinityConfig.Command.BOUND ||
            command === AffinityConfig.Command.UNBIND) {
            boundKey = getAffinityKeyFromMessage(affinityConfig.affinityKey, argument);
        }
    }
    const channelRef = channelFactory.getChannelRef(boundKey);
    channelRef.activeStreamsCountIncr();
    return {
        boundKey,
        channelRef,
    };
}
/**
 * Handle channel affinity and streams count after call is done.
 * @param channelFactory The channel management factory.
 * @param channelRef ChannelRef instance that contains a real grpc channel.
 * @param path Method path.
 * @param boundKey Affinity key bound to a channel.
 * @param responseMsg Response proto message.
 */
function postProcess(channelFactory, channelRef, path, boundKey, 
// tslint:disable-next-line:no-any protobuf message
responseMsg) {
    if (!channelFactory || !responseMsg)
        return;
    const affinityConfig = channelFactory.getAffinityConfig(path);
    if (affinityConfig && affinityConfig.command) {
        const command = affinityConfig.command;
        if (command === AffinityConfig.Command.BIND) {
            const affinityKey = getAffinityKeyFromMessage(affinityConfig.affinityKey, responseMsg);
            channelFactory.bind(channelRef, affinityKey);
        }
        else if (command === AffinityConfig.Command.UNBIND) {
            channelFactory.unbind(boundKey);
        }
    }
    channelRef.activeStreamsCountDecr();
}
/**
 * Retrieve affinity key specified in the proto message.
 * @param affinityKeyName affinity key locator.
 * @param message proto message that contains affinity info.
 * @return Affinity key string.
 */
function getAffinityKeyFromMessage(affinityKeyName, 
// tslint:disable-next-line:no-any protobuf message
message) {
    if (affinityKeyName) {
        let currMessage = message;
        const names = affinityKeyName.split('.');
        let i = 0;
        for (; i < names.length; i++) {
            if (currMessage[names[i]]) {
                // check if the proto message is generated by protobufjs.
                currMessage = currMessage[names[i]];
            }
            else {
                // otherwise use jspb format.
                const getter = 'get' + names[i].charAt(0).toUpperCase() + names[i].substr(1);
                if (!currMessage || typeof currMessage[getter] !== 'function')
                    break;
                currMessage = currMessage[getter]();
            }
        }
        if (i !== 0 && i === names.length)
            return currMessage;
    }
    console.error(util.format('Cannot find affinity value from proto message using affinity_key: %s.', affinityKeyName));
    return '';
}
//# sourceMappingURL=index.js.map