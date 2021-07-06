"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
class BaseFilter {
    async sendMetadata(metadata) {
        return metadata;
    }
    async receiveMetadata(metadata) {
        return metadata;
    }
    async sendMessage(message) {
        return message;
    }
    async receiveMessage(message) {
        return message;
    }
    async receiveTrailers(status) {
        return status;
    }
}
exports.BaseFilter = BaseFilter;
//# sourceMappingURL=filter.js.map