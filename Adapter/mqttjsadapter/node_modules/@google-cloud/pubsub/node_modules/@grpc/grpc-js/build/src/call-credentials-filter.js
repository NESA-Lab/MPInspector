"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const filter_1 = require("./filter");
class CallCredentialsFilter extends filter_1.BaseFilter {
    constructor(channel, stream) {
        super();
        this.channel = channel;
        this.stream = stream;
        this.channel = channel;
        this.stream = stream;
        const splitPath = stream.getMethod().split('/');
        let serviceName = '';
        /* The standard path format is "/{serviceName}/{methodName}", so if we split
         * by '/', the first item should be empty and the second should be the
         * service name */
        if (splitPath.length >= 2) {
            serviceName = splitPath[1];
        }
        /* Currently, call credentials are only allowed on HTTPS connections, so we
         * can assume that the scheme is "https" */
        this.serviceUrl = `https://${stream.getHost()}/${serviceName}`;
    }
    async sendMetadata(metadata) {
        const channelCredentials = this.channel.credentials._getCallCredentials();
        const streamCredentials = this.stream.getCredentials();
        const credentials = channelCredentials.compose(streamCredentials);
        const credsMetadata = credentials.generateMetadata({ service_url: this.serviceUrl });
        const resultMetadata = await metadata;
        resultMetadata.merge(await credsMetadata);
        return resultMetadata;
    }
}
exports.CallCredentialsFilter = CallCredentialsFilter;
class CallCredentialsFilterFactory {
    constructor(channel) {
        this.channel = channel;
    }
    createFilter(callStream) {
        return new CallCredentialsFilter(this.channel, callStream);
    }
}
exports.CallCredentialsFilterFactory = CallCredentialsFilterFactory;
//# sourceMappingURL=call-credentials-filter.js.map