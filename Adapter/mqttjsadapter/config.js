const jwt = require('jsonwebtoken');
const fs = require('fs');
const path = require('path');
const readSync = require("node-yaml").readSync


const PasswordGenerator = {
    fixed: (password) => {
        return password.data;
    },
    jwt: (password) => {
        let token = {
            iat: parseInt(Date.now() / 1000),
            exp: parseInt(Date.now() / 1000) + 20 * 60, // 20 minutes
          };
          Object.assign(token, password.token)
          const privateKey = fs.readFileSync(password.privateKeyFile);
            return jwt.sign(token, privateKey, {algorithm: password.algorithm});
    }
}

const PubPayloadGenerator = {
    fixed: (sub) => {
        return sub.data;
    }
}

function UseAccessor(client, generator, field, filename) {
    const raw = client[field]
    raw.filename = filename
    const manager = generator[raw.method];

    Object.defineProperty(client, field, {
        get() {
            return manager(raw);
        }
    })
}

function LoadClientFromConfig(filename) 
{
    const client = readSync(filename);
    
    UseAccessor(client, PasswordGenerator, "password", filename);
    UseAccessor(client["pub_defaults"], PubPayloadGenerator, "payload", filename);

    return client;
}

// Create a Cloud IoT Core JWT for the given project id, signed with the given
// private key.
// [START iot_mqtt_jwt]
function createJwt(projectId, privateKeyFile, algorithm) {
    // Create a JWT to authenticate this device. The device will be disconnected
    // after the token expires, and will have to reconnect with a new token. The
    // audience field should always be set to the GCP project id.
    const token = {
      iat: parseInt(Date.now() / 1000),
      exp: parseInt(Date.now() / 1000) + 20 * 60, // 20 minutes
      aud: projectId,
    };
    const privateKey = fs.readFileSync(privateKeyFile);
    return jwt.sign(token, privateKey, {algorithm: algorithm});
  }
  // [END iot_mqtt_jwt]



const Config = ()=>{
    return {
        LoadClientFromConfig: LoadClientFromConfig
    }
}

module.exports = Config;