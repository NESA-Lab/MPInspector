const jwt = require('jsonwebtoken');
const fs = require('fs');
const path = require('path');
const yaml = require("node-yaml")
const readSync = yaml.readSync
const writeSync = yaml.writeSync
const CryptoJS = require("crypto-js");

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
          let privateKey = password.privateKey
          if(password.privateKeyFile) {
              privateKey = fs.readFileSync(password.privateKeyFile);
          }
          return jwt.sign(token, privateKey, {algorithm: password.algorithm});
    },
    tuya_publish: (password) => {
        const key_string = password.localKey
        const msg = password.data
        const key = CryptoJS.enc.Utf8.parse(key_string)
        const enc = CryptoJS.AES.encrypt(msg, key, { 
                                         padding: CryptoJS.pad.Pkcs7, 
                                         mode: CryptoJS.mode.ECB}).toString()
        const tobe_signed = 'data=' + enc + '||pv=2.1||' + key_string
        const sig = CryptoJS.MD5(tobe_signed).toString(CryptoJS.enc.Hex)
        return '2.1' + sig.slice(8,24) + enc;
    },
    ali_password: (password) => {
        return CryptoJS.HmacSHA1(
            `clientId${password.deviceId}deviceName${password.deviceName}productKey${password.productKey}`,
            password.key
        ).toString(CryptoJS.enc.Hex).toUpperCase()
    },
    use_raw: (password) => {
        return password.raw
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

function LoadClientFromConfigObject(client, filename)
{
    if(!filename)
    {
        filename = "."
    }
    if(client.device){
        client = client.device
    }
    UseAccessor(client, PasswordGenerator, "password", filename);
    UseAccessor(client["pub_defaults"], PasswordGenerator, "payload", filename);

    return client;
}

function LoadClientFromConfig(filename) 
{
    const client = readSync(filename);
    
    return LoadClientFromConfigObject(client, filename)
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
        LoadClientFromConfig: LoadClientFromConfig,
        YamlWrite: writeSync,
        LoadClientFromConfigObject, LoadClientFromConfigObject
    }
}

module.exports = Config;