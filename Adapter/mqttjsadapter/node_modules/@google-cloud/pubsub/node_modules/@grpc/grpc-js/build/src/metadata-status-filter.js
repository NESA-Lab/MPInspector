"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const constants_1 = require("./constants");
const filter_1 = require("./filter");
class MetadataStatusFilter extends filter_1.BaseFilter {
    async receiveTrailers(status) {
        // tslint:disable-next-line:prefer-const
        let { code, details, metadata } = await status;
        if (code !== constants_1.Status.UNKNOWN) {
            // we already have a known status, so don't assign a new one.
            return { code, details, metadata };
        }
        const metadataMap = metadata.getMap();
        if (typeof metadataMap['grpc-status'] === 'string') {
            const receivedCode = Number(metadataMap['grpc-status']);
            if (receivedCode in constants_1.Status) {
                code = receivedCode;
            }
            metadata.remove('grpc-status');
        }
        if (typeof metadataMap['grpc-message'] === 'string') {
            details = decodeURI(metadataMap['grpc-message']);
            metadata.remove('grpc-message');
        }
        return { code, details, metadata };
    }
}
exports.MetadataStatusFilter = MetadataStatusFilter;
class MetadataStatusFilterFactory {
    constructor(channel) {
        this.channel = channel;
    }
    createFilter(callStream) {
        return new MetadataStatusFilter();
    }
}
exports.MetadataStatusFilterFactory = MetadataStatusFilterFactory;
//# sourceMappingURL=metadata-status-filter.js.map