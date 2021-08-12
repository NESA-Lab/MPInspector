const { assert } = require('console');

const qosMapping = {
    "AT_MOST_ONCE": 0,
    "AT_LEAST_ONCE": 1,
    "EXACTLY_ONCE": 2
}

const userFilledTerms = {
    "alitcp.password.key": [["CONNECT", "password", "encry_key"]],
    "gcp.jwt.key": [[  "CONNECT", "password", "encry_key" ]],
    "tuya.publish.key": [["PUBLISH", "payload", "encry_key"]],
    "host": [["SERVER", "host"]],
    "port": [["SERVER", "port"]],
    "secureProtocol": [["SERVER", "secureProtocol"]],
    "mqttVersion": [["SERVER", "mqttVersion"]],
    "protocol": [["SERVER", "protocol"]],
    "password.raw": [["CONNECT", "password", "raw"]]
}

const tryModeifyString = (str) => {
    let code = str.charCodeAt(str.length-1);
    return str.slice(0, str.length-1) + String.fromCodePoint(code^1)
}

const tryModifyNumber = (num) => {
    return num + 1;
}

const tryModifyStringOrNumber = (obj) => {
    let number = undefined
    if(Number.isInteger(obj)) number = obj;
    if(!isNaN(parseInt(obj))) number = parseInt(obj);


    if(number === undefined) return tryModeifyString(obj)
    else return tryModifyNumber(number)
}

const updateDeviceField = (obj, locator, value) => {
    let cur = obj
    let parent = undefined // parent object of cur
    let depth = 0
    const max_depth = locator.length
    while(depth < max_depth)
    {
        if(Array.isArray(cur)) {
            let max_num_keys = 0
            for(let kv of cur) {
                let found = false
                max_num_keys = Math.max(max_num_keys, Object.keys(kv).length)
                for(const k of Object.keys(kv))
                {
                    const v = kv[k]
                    if(k === locator[depth])
                    {
                        parent = kv;
                        cur = v;
                        found = true
                        break
                    }
                }
                if(found) break;
                console.assert(depth === max_depth-1)
                if(cur.length === 1 && max_num_keys > 0)
                {
                    parent = cur[0]
                    cur = undefined
                }
                else {
                    parent = {
                        [locator[depth]]: value
                    }
                    cur.push(parent)
                    cur = value
                }
                
                
            }
        }
        else {
            parent = cur;
            cur = cur[locator[depth]]
        }
        ++depth;
    }
    --depth;

    if(Array.isArray(cur) && !Array.isArray(value)) {
        parent[locator[depth]] = [ value, ]
    }
    else {
        parent[locator[depth]] = value
    }
    return {
        valueBeforeChanged: cur,
        valueAfterChanged: parent[locator[depth]]
    }
}


const fillInMissingTerms = (obj, name, value) => {
    const locators = userFilledTerms[name]
    console.assert(locators !== undefined)
    for(let locator of locators)
    {
        updateDeviceField(obj, locator, value)
    }
}


class ParserContext
{
    constructor() {
        this.env = {
            data: new Array,
            push: (x)=> this.env.data.push(x),
            pop: () => this.env.data.pop(),
            back: () => this.env.data[this.env.data.length-1]
        }

        this.encryptedTerms = []
        this.encryptedFields = []
    }

    error(msg) {
        console.error(msg)
    }

    addEncryptedTerm(method, key, value) {
        this.encryptedTerms.push({
            env: [ ...this.env.data, key ],
            val: value,
            method: method
        })
    }
    addEncryptedField(value) {
        this.encryptedFields.push({
            env: [...this.env.data],
            val: value
        })
    }
}

let ctx = new ParserContext

const parseTrafficAnalysisResult = (source, raw, opts) =>
{
    ctx = new ParserContext; ctx.raw = raw; ctx.opts = opts || {}
    const device = source.SERVER || {}

    const CONNECT = simplify(source.CONNECT)
    const SUBSCRIBE = simplify(source.SUBSCRIBE)
    const UNSUBSCRIBE = simplify(source.UNSUBSCRIBE)
    const PUBLISH = simplify(source.PUBLISH)

    // connect
    ctx.env.push("CONNECT")
        device.platform_name = source.platform;
        device.username = parseIdOrUsername(CONNECT.username, source.platform, 'username')
        device.clientId = parseIdOrUsername(CONNECT.clientID, source.platform, 'clientId')
        device.password = parsePassword(CONNECT.password)
        
        for(let key of Object.keys(device))
        {
            if(device[key] == undefined) {
                delete device[key]
            }
        }
    ctx.env.pop()

    // subscribe
    ctx.env.push("SUBSCRIBE")
        device.sub_defaults = {
            qos: parseQos(SUBSCRIBE.topics),
            topic: parseTopic(SUBSCRIBE.topics)
        }
    ctx.env.pop()

    // unsubscribe
    ctx.env.push("UNSUBSCRIBE")
        device.unsub_defaults = {
            topic: parseTopic(UNSUBSCRIBE.topics),
            qos: parseQos(UNSUBSCRIBE.topics)
        }
    ctx.env.pop()

    // publish
    ctx.env.push("PUBLISH")
        device.pub_defaults = {
            topic: parseTopic(PUBLISH.topic),
            qos: parseQos(PUBLISH.topic),
            payload: parsePayload(PUBLISH.payload),
        }
    ctx.env.pop()
    return {
        device: device,
        encryptedTerms: ctx.encryptedTerms,
        encryptedFields: ctx.encryptedFields
    }
}

const iterateArray = function* (arr) {
    for(let item of arr)
    {
        const keys = Object.keys(item)
        assert(keys.length == 1)
        yield [ keys[0], item[keys[0]] ]
    }
    return;
}

// make array of key-value pairs into an object, for example:
//    input: [ {"a":1}, {"b":{}}, {"c": "hi"} ]
//    output: { a:1, b:{}, c:"hi" }
const simplify = (obj) => {
    result = {}
    for(let [k, v] of iterateArray(obj)) {
        result[k] = v
    }
    return result
}

const parseTopic = (topic)=> {
    const result = []
    for(let subtopic of topic) {
        const keys = Object.keys(subtopic)
        assert(keys.length == 1)
        const key = keys[0]

        if(key !== "qos") {
            result.push(subtopic[key])
        }
    }

    return result.join('/')
}


const parseQos = (topic) => {
    for(let subtopic of topic) {
        const keys = Object.keys(subtopic)
        assert(keys.length == 1)
        const key = keys[0]

        if(key === "qos") {
            return qosMapping[subtopic[key]]
        }
    }
    return 1;
}

// payload takes 3 forms
//   - empty array
//   - array of a single string
//   - array of encryption descriptor
const parsePayload = (payload) => {
    if(payload === undefined || payload.length === 0)
    {
        return {
            method: 'fixed',
            data: "fixed message"
        }
    }
    else if(typeof(payload[0]) === 'string') {
        return {
            method: 'fixed',
            data: payload[0]
        }
    }
    else {
        // tuya
        if(payload.length === 2 &&
            payload[0].method === "senc" &&
            payload[1].method === "md5") {
                // ctx.addEncryptedTerm()
                const info = payload[0]
                return {
                    method: "tuya_publish",
                    localKey: info.encry_key,
                    data: JSON.stringify({
                        data: {
                            devId: info.encry_term,
                            "dps": {
                                "5": "1c1c1c0138001b"
                            }
                        },
                        "protocol": 5,
                        "s": 45,
                        "t": 1568297715
                    })
                }
            }
        
    }
    ctx.error("Unkown payload type")
    return {
        method: 'fixed',
        data: "fixed message"
    }
}

// parse clientId or username
//   1. array of key-value pairs, the values are joined by '/'
//     the value is an **array** with one string
//   2. array containing only one string
//   3. array that contains several objects, an object has the keyword "method"
const parseIdOrUsername = (id, platform, idOrUsername) => {
    if(id === undefined) {
        return undefined
    }
    if(typeof id == 'string') {
        return id
    }

    if(Array.isArray(id)) {
        if(id.length === 0) {
            return ""
        }

        if(typeof id[0] === 'string') {
            return id[0]
        }

        const keys = []
        const vals = []
        const obj = {}
        let method = "uri"
        for(let [key, val] of iterateArray(id)) {
            if(key === "method") {
                method = val
            }
            else {
                keys.push(key)
                vals.push(val)
                obj[key] = val
            }
        }

        if(method === "uri") {
            const uris = []
            for(let uri of vals) {
                uris.push(uri[0])
            }
            if(platform === "alitcp") {
                if(idOrUsername === 'username') {
                    return uris.join('&')
                }
                return uris.join("|") + "|"
            }
            else if(platform === "bosch") {
                return uris.join("@")
            }
            return uris.join("/")
        }
        else if(method === "urlencode")
        {
            const params = []
            for(let [name, val] of iterateArray(obj.params)) {
                params.push(`${name}=${val}`)
            }
            return `${obj.url}${params.join('&')}`
        }
    }
}

// encrytion decriptor, identified by keyword
//    - typ="jwt"          gcp JWT password
//    - method="sas"       azure sas password
//    - method="senc"      tuya publish payload (part 1)
//    - method="md5"       tuya publish payload (part 2)
//    - method="hmacsha1"  alitcp password
const parseEncryption = (enc) => {
    let result = undefined;

    if(enc.typ !== undefined) {
        if(enc.typ === "JWT") {
            const token = { ...enc }
            delete token.typ
            delete token.alg;
            delete token.iat;
            delete token.exp;
            delete token.encry_key;

            for(let k of Object.keys(token))
            {
                ctx.addEncryptedTerm("jwt", k, token[k])
            }

            result = {
                method: "jwt",
                token: token,
                algorithm: enc.alg,
                privateKey: enc.encry_key
            }
        }
        else ctx.error(`Unknown encryption method: ${enc.typ}`)
    }

    else if(enc.method !== undefined)
    {
        if(enc.method === "use_raw") {
            result = {
                method: "use_raw",
                raw: enc.raw
            }
        }
        else if(enc.method === "senc") {
            //TODO
        }
        else if(enc.method === "md5") {
            //TODO
        }
        else if(enc.method === "hmacsha1") {
            const terms = simplify(enc.encry_term)
            result = {
                method: "ali_password",
                key: enc.encry_key,
                deviceId: terms.deviceId[0],
                deviceName: terms.deviceName[0],
                productKey: terms.productKey[0]
            }
            
        }
        else if(enc.method === "sas") {
            const terms = simplify(enc.encry_term)
            const sr = encodeURIComponent(terms.sr[0])
            const se = encodeURIComponent(terms.se[0])
            const sig = encodeURIComponent(terms.sig[0])

            ctx.addEncryptedTerm("sas", "sr", sr)
            ctx.addEncryptedTerm("sas", "se", se)
            ctx.addEncryptedTerm("sas", "sig", sig)

            result = {
                method: "fixed",
                data: `SharedAccessSignature sr=${sr}&se=${se}&sig=${sig}`
            }
        }
        else ctx.error(`Unknown encryption method: ${enc.method}`)
    }

    if(result !== undefined)
    {
        if(enc.raw)
        {
            result.raw = enc.raw
        }
    }
    return result
}

// password field for CONNECT
//    - array with only one string
//    - object with keyword "typ"
//    - array with only one object, the object has keyword "method"
const parsePassword = (passwd) => {
    if(passwd === undefined) {
        return undefined
    }
    if(Array.isArray(passwd)) {
        if(passwd.length === 0) {
            return undefined
        }
        passwd = passwd[0]

        if(typeof passwd === "string") {
            return passwd
        }
        // otherwise the first item of the array is an encryption descriptor
    }

    
    // if it is an encryption descriptor
    ctx.env.push("password")
        const password = parseEncryption(passwd)
        if(ctx.raw?.password && !password.raw)
        {
            password.raw = ctx.raw.password
        }
        if(ctx.opts.enforcePasswordUseRaw)
        {
            password.method = "use_raw"
            password.raw = ctx.opts.passwordRaw || password.raw
        }
        if(password.raw)
        {
            ctx.addEncryptedField(password.raw)
        }
    ctx.env.pop()
    return password
    
}


module.exports = {
    fillInMissingTerms: fillInMissingTerms,
    parseTrafficAnalysisResult: parseTrafficAnalysisResult,
    tryModifyStringOrNumber: tryModifyStringOrNumber,
    updateDeviceField: updateDeviceField,
}