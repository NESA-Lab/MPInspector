"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const metadata_1 = require("./metadata");
/**
 * A class that represents a generic method of adding authentication-related
 * metadata on a per-request basis.
 */
class CallCredentials {
    /**
     * Creates a new CallCredentials object from a given function that generates
     * Metadata objects.
     * @param metadataGenerator A function that accepts a set of options, and
     * generates a Metadata object based on these options, which is passed back
     * to the caller via a supplied (err, metadata) callback.
     */
    static createFromMetadataGenerator(metadataGenerator) {
        return new SingleCallCredentials(metadataGenerator);
    }
    static createEmpty() {
        return new EmptyCallCredentials();
    }
}
exports.CallCredentials = CallCredentials;
class ComposedCallCredentials extends CallCredentials {
    constructor(creds) {
        super();
        this.creds = creds;
    }
    async generateMetadata(options) {
        const base = new metadata_1.Metadata();
        const generated = await Promise.all(this.creds.map((cred) => cred.generateMetadata(options)));
        for (const gen of generated) {
            base.merge(gen);
        }
        return base;
    }
    compose(other) {
        return new ComposedCallCredentials(this.creds.concat([other]));
    }
}
class SingleCallCredentials extends CallCredentials {
    constructor(metadataGenerator) {
        super();
        this.metadataGenerator = metadataGenerator;
    }
    generateMetadata(options) {
        return new Promise((resolve, reject) => {
            this.metadataGenerator(options, (err, metadata) => {
                if (metadata !== undefined) {
                    resolve(metadata);
                }
                else {
                    reject(err);
                }
            });
        });
    }
    compose(other) {
        return new ComposedCallCredentials([this, other]);
    }
}
class EmptyCallCredentials extends CallCredentials {
    generateMetadata(options) {
        return Promise.resolve(new metadata_1.Metadata());
    }
    compose(other) {
        return other;
    }
}
//# sourceMappingURL=call-credentials.js.map