

const fs = require('fs');
const mqtt = require('mqtt');
const mqttPacket = require('mqtt-packet');
const _configManger = require('./config')()
const LoadClientFromConfig = _configManger.LoadClientFromConfig //loadconfig
const YamlWrite = _configManger.YamlWrite
const LoadClientFromConfigObject = _configManger.LoadClientFromConfigObject
const express = require('express');
const _parseTrafficAnalysisResult = require('./parse_analysis_result');
const parseTrafficAnalysisResult = _parseTrafficAnalysisResult.parseTrafficAnalysisResult;
const fillInMissingTerms = _parseTrafficAnalysisResult.fillInMissingTerms;
const tryModifyStringOrNumber = _parseTrafficAnalysisResult.tryModifyStringOrNumber;
const updateDeviceField = _parseTrafficAnalysisResult.updateDeviceField
const app = express()
const port = 9123  //mqtt5
const log_dir = './log/'






const getTimeString = () => {
    const d = new Date();
    return `${d.getMonth()}.${d.getDate()}.${d.getHours()}.${d.getMinutes()}`
}


function sendPacket (client, packet, cb) {
    //client.emit('packetsend', packet)
  
    var result = mqttPacket.writeToStream(packet, client.stream, client.options)
  
    if (!result && cb) {
      client.stream.once('drain', cb)
    } else if (cb) {
      cb()
    }
  }

class MyMqttClient {

    setDevice(device, log) {
        if(device == null) return;
        // console.log('device properties: ', device.properties);
        const additional_connect_args = device.additional_connect_args || {}
        const connectionArgs = {
            host: device.host,
            port: device.port,
            protocolVersion: device.mqttVersion,
            clientId: device.clientId,
            username: device.username,
            password: device.password,
            protocol: device.protocol,
            secureProtocol: device.secureProtocol,
            reconnectPeriod:  device.reconnectPeriod || 9999999,
            connectTimeout: 20000, // 
            properties: device.properties,
            rejectUnauthorized: device.rejectUnauthorized,
			//MQTT5.0
			
            // MQTT 5.0 auth
            //authPacket: device.authPacket,

            // stop sending connect
            x_neverSendConnect: true,
            // stop store packets
            x_overrideStoreBeforeConnect: true,

            ...additional_connect_args
        };
        console.log(connectionArgs)

        this.device = device
        const client = mqtt.connect(connectionArgs);
        this.client = client
        this.stream = this.client.stream;
        this.options = this.client.options;
        const platform = device.platform_name

        const default_log = (x)=>{ console.log(x) }
        log = log || default_log
        this.log = log
        this.platform_name = platform
        

        client.on('connect', success => {
            if (!success) {
              log(`Client not connected...`);
            }
            else {
              log(`[${platform}]: connected`);
            }
          });
          
          client.on('error', err => {
            log(`[${platform}]: error: ${err}`);
            if(this.resolve_fn) {
                this.resolve_fn(this.detail_err_ret? `error: ${err}` : 'error')
                this.resolve_fn = null;
                this.resolve_handled = true
            }
          });
      
          client.on('close', () => {
            log(`[${platform}]: closed`);
            this.closed = true
            if(this.resolve_fn) {
                this.resolve_fn('closed')
                this.resolve_fn = null;
                this.resolve_handled = true
            }
          });
          
          this.message_buffer = []
          client.on('message', (topic, message) => {
            let messageStr = `[${this.platform_name}]: Message received: `;  
            messageStr += 'topic: ' + topic + '  payload ' + Buffer.from(message, 'base64').toString('ascii');
            log(messageStr);
            this.message_buffer.push({
                topic: topic,
                message: message
            })
          });
          this.pktbuf = new Buffer.from([]);
          
          const handle_msg = pkt =>{
            try{
                this.pktbuf = Buffer.concat([this.pktbuf, mqttPacket.generate(pkt)])
            }catch(e)
            {
                
            }
          }
          
        client.on('packetreceive', pkt =>{
            handle_msg(pkt)
        })
        client.on('x_onsend', pkt =>{
            handle_msg(pkt)
        })
        client.on('x_onstore', pkt =>{
            handle_msg(pkt)
        })


        }

        

        async pubrecv()
        {
            const res = this.message_buffer
            this.message_buffer = []
            return res;
        }

        async connect()
        {
            if(this.closed)
            {
                await this.reset()
            }
            return new Promise((resolve)=>{
                if(this.closed) {
                    resolve('closed')
                    return;
                }
                // before resolve, the close may be triggered
                // proxy close
                this.resolve_fn = resolve
                this.resolve_handled = false

                this.client.once('connect', success => {
                    this.resolve_fn = null
                    if(this.resolve_handled) {
                        resolve('something happend but we received callback, success:' + JSON.stringify(success))
                        return;
                    }
                    if(success)
                    {
                        if(success.returnCode) {
                            resolve('closed')
                        }
                        else
                        {
                            resolve('connack')
                        }
                    }
                    else{
                        resolve('connack') 
                    }
                    
                  });
                  
                sendPacket(this.client, this.client._lastConnectPacket)
            })
        }

        async disconnect()
        {
            return new Promise((resolve)=>{
                if(this.closed || !this.client) {
                    resolve('closed')
                    this.log(`[${this.platform_name}]: already closed`)
                    return;
                }
                this.resolve_fn = resolve
                this.resolve_handled = false
                this.client.endStreamCb(()=>{
                    this.resolve_fn = null
                    if(!this.resolve_handled) {
                        resolve("closed")
                        this.log(`[${this.platform_name}]: discon, closed`)
                    }
                    else {
                        this.log(`[${this.platform_name}]: discon handled by others`)
                    }
                })
            })
        }

        async reset()
        {
            this.client.removeAllListeners('close')
            this.client.removeAllListeners('error')
            this.client.removeAllListeners('message')
            this.closed = false;
            
            this.setDevice(this.device, this.log)
            return "ok";
        }

        async publish()
        {
            return new Promise((resolve)=>{
                if(this.closed) {
                    resolve('closed')
                    return;
                }

                this.resolve_fn = resolve
                this.resolve_handled = false
                const pub = this.device.pub_defaults
				console.log(pub.properties)
                this.client.publish(pub.topic, pub.payload,  {qos: pub.qos, properties: pub.properties, enforceSend: true}, (error, pkt)=>{
                    
                    if(!this.resolve_handled) 
                    {
                        if(!error){
                            this.resolve_fn = null
                            resolve('success')
                        }
                        else {
                            this.client.once('closed', ()=>{
                                resolve = null;
                            })
                            setTimeout(()=>{
                                if(!this.resolve_handled && resolve)
                                {
                                    this.log(`[${this.platform_name}]pub failed with error: ` + JSON.stringify(error) );
                                    this.resolve_fn = null
                                    resolve('error')
                                }

                            }, 1000)
                        }
                    }
                })
            })
        }

        async savelog()
        {
            fs.writeFileSync(log_dir + this.platform_name + getTimeString(), this.pktbuf)

            return 'success'
        }

        async subscribe()
        {
            return new Promise((resolve)=>{
                if(this.closed) {
                    resolve('closed')
                    return;
                }

                this.resolve_fn = resolve
                this.resolve_handled = false
                const sub = this.device.sub_defaults
                const subargs = {properties: sub.properties, qos: sub.qos, enforceSend: true}
                for(let argkey of Object.keys(subargs))
                {
                    if(subargs[argkey] === undefined){
                        delete subargs[argkey]
                    }
                }
                this.client.subscribe(sub.topic, subargs, (error)=>{
                    
                    if(!this.resolve_handled) 
                    {
                        if(!error){
                            this.resolve_fn = null
                            resolve('success')
                        }
                        else {
                            this.client.once('closed', ()=>{
                                resolve = null;
                            })
                            setTimeout(()=>{
                                
                                if(!this.resolve_handled && resolve)
                                {
                                    this.log(`[${this.platform_name}]sub failed with error: ` + JSON.stringify(error) );
                                    this.resolve_fn = null
                                    resolve('error')
                                }
                            }, 1000)
                        }
                    }
                })
            })
        }

            

        async unsubscribe()
        {
            return new Promise((resolve)=>{
                if(this.closed) {
                    resolve('closed')
                    return;
                }

                this.resolve_fn = resolve
                this.resolve_handled = false
                const unsub = this.device.unsub_defaults || this.device.sub_defaults
                this.client.unsubscribe(unsub.topic, {properties: unsub.properties}, (error)=>{
                    
                    if(!this.resolve_handled) 
                    {
                        if(!error){
                            this.resolve_fn = null
                            resolve('success')
                        }
                        else {
                            this.client.once('closed', ()=>{
                                resolve = null;
                            })
                            setTimeout(()=>{
                                if(!this.resolve_handled && resolve)
                                {
                                    this.log(`[${this.platform_name}]unsub failed with error: ` + JSON.stringify(error) );
                                    this.resolve_fn = null
                                    resolve('error')
                                }
                            }, 1000)
                        }
                    }
                })
            })
        }
}

let client = new MyMqttClient
let config = {}
let debug = true

app.use(function(req, res, next){
    var data = "";
    req.on('data', function(chunk){ data += chunk})
    req.on('end', function(){
       req.rawBody = data;
       next();
    })
 })

// for test
app.get('/', (req, res)=>{
    //res.send(fs.readFileSync("index.html"))
    res.sendFile("index.html", { root: __dirname })
})

app.post('/mqtt/config/fromTrafficAnalysis', async (req, res, next) => {
    console.log("[Traffic Analysis] Load Json")
    const conf = JSON.parse(req.rawBody)
    conf.data = JSON.parse(conf.data0); //
    conf.raw = JSON.parse(conf.data1)
    
    if(conf.data.SERVER === undefined)
    {
        conf.data.SERVER = {}
    }
    console.log(conf)

    console.log("[Traffic Analysis] Filling missing terms")
    const userfillkw = "userFilledTerms."
    const parsingoptskw = "parsingOptions."
    const missingTerms = {}
    const parsingOpts = {}
    for (let propName in conf.args) {
        if (conf.args.hasOwnProperty(propName)) {
            if(propName.startsWith(userfillkw)) {
                const k = propName.slice(userfillkw.length)
                const v = conf.args[propName]
                fillInMissingTerms(conf.data, k, v)
                missingTerms[k] = v
            }
            if(propName.startsWith(parsingoptskw)) {
                const k = propName.slice(parsingoptskw.length)
                const v = conf.args[propName]
                parsingOpts[k] = v
            }
        }
    }
    console.log(missingTerms)

    console.log("[Traffic Analysis] Start parsing")
    config = parseTrafficAnalysisResult(conf.data, conf.raw, parsingOpts)
    config.deviceJson = conf.data
    config.deviceArgs = conf.args
    config.parsingOpts = parsingOpts

    if(debug && conf.args.debug_result_yaml_save_path) {
        // remove `undefined` fields
        const c = JSON.parse(JSON.stringify(config))
        YamlWrite(conf.args.debug_result_yaml_save_path, c)
    }
    res.send(config)
    // console.log("Parsing finished")
})

// app.get('/mqtt/config/fill', async (req, res, next) => {
//     for (var propName in req.query) {
//         if (req.query.hasOwnProperty(propName)) {
//             console.log(propName, req.query[propName]);
//         }
//     }
// })

app.get('/mqtt/config/initClient', async (req, res, next) => {
    client = new MyMqttClient
    const device = LoadClientFromConfigObject(config.device)
    await client.setDevice(device)
    res.send('ok')
})

app.get('/mqtt/config/analyze/fields', async (req, res, next) => {
    console.log(config.normalFields)
    const targets = [...config.encryptedFields, ...config.normalFields]
    console.log(`[Traffic Analysis] Analyzing encrypted fields, ${targets.length} is found`)
    const deepclone = (x) => JSON.parse(JSON.stringify(x))
    const originalDeviceJson = config.deviceJson
    const analysis = []
    const steplog = (x, error) => {
        if(error) console.error("   X -> " + x)
        else console.log("     -> " + x)
    }

    const stages = [
        {   name: 'connect',
            goodResponse: 'connack'
        },

        {   name: 'publish',
            goodResponse: 'success'
        },

        {   name: 'subscribe',
            goodResponse: 'success'
        },

        {   name: 'unsubscribe',
            goodResponse: 'success'
        },

        {   name: 'disconnect',
            goodResponse: 'disconnect'
        }
    ]

    const runAllStages =  async (ends) => {
        for(let stage of stages) {
            const response = await client[stage.name]()
            if(response === stage.goodResponse) {
                steplog(stage.name + ": " + response)
            }
            else {
                steplog(stage.name + ": " + response, true)
                return {
                    success: false,
                    stage: stage,
                    response: response
                }
            }
            if(ends && ends.toLowerCase() === stage.name.toLowerCase())
            {
                break;
            }
        }
        return {
            success: true,
            stage: ends,
            response: ""
        }
    }

    const reset = async () => {
        const resetresult = await client.reset(); steplog("reset: " + resetresult)
    }
    for(let term of targets) {
        console.log("Analysing")
        console.log(term)
        let newConfig = null
        const getOutputTerm = () => {
            const t = {...term}
            delete t.edit
            return t
        }
        const newDeviceJson = deepclone(originalDeviceJson)
        if(term.encrypted)
        {
            
            // update password field, make sure it uses raw
            updateDeviceField(newDeviceJson, [...term.env, "raw"], term.val)
            updateDeviceField(newDeviceJson, [...term.env, "method"], "use_raw")
            newConfig = parseTrafficAnalysisResult(newDeviceJson)
        }
        else {
            newConfig = config
        }
        await client.disconnect()
        client = new MyMqttClient;
        client.detail_err_ret = true
        client.setDevice(LoadClientFromConfigObject(deepclone(newConfig.device))); await reset();

        // if the server accept the replayed message
        // it means the message has no timestamp
        let result = await runAllStages(term.env[0])
        if(!result.success) {
            analysis.push({
                field: getOutputTerm(),
                newValue: term.val,
                replayable: false,
                modifiable: null,
                stage: result.stage.name,
                reason: result.response
            })
            continue
        }

        const newValue = tryModifyStringOrNumber(term.val)
        if(term.encrypted)
        {
            updateDeviceField(newDeviceJson, [...term.env, "raw"], newValue)
            newConfig = parseTrafficAnalysisResult(newDeviceJson, undefined)
        }
        else {
            newConfig = config
            term.edit.update(newValue)
        }

        await client.disconnect()
        client = new MyMqttClient;
        client.detail_err_ret = true
        client.setDevice(LoadClientFromConfigObject(deepclone(newConfig.device))); await reset();
        
        // the server will check the timestamp
        result = await runAllStages(term.env[0])
        if(!term.encrypted) term.edit.restore()
        if(result.success) {
            analysis.push({
                field: getOutputTerm(),
                newValue: newValue,
                replayable: true,
                modifiable: true,
                stage: result.stage.name,
                reason: result.response
            })
        }
        else {
            analysis.push({
                field: getOutputTerm(),
                newValue: newValue,
                replayable: true,
                modifiable: false,
                stage: result.stage.name,
                reason: result.response
            })
        }
    }

    // config.device = originalDevice
    res.send(analysis)
})

app.get('/mqtt/config/analyze/terms', async (req, res, next) => {
    console.log(`[Traffic Analysis] Analyzing encrypted terms, ${config.encryptedTerms.length} is found`)
    const deepclone = (x) => JSON.parse(JSON.stringify(x))
    const originalDeviceJson = config.deviceJson
    const rejects = {}
    const steplog = (x, error) => {
        if(error) console.error("   X -> " + x)
        else console.log("     -> " + x)
    }

    const reportRejected = (encryptedTerm, stage, reason, newValue, success) => {
        rejects.push({
            rejected: !success,
            stage: stage,
            reason: reason,
            newValue: newValue,
            encryptedTerm: encryptedTerm,
        })
    }

    for(let term of [...config.encryptedTerms, ...config.normalFields]) {
        const newValue = tryModifyStringOrNumber(term.val)
        if(term.encrypted)
        {

        }
        const newDeviceJson = deepclone(originalDeviceJson)
        updateDeviceField(newDeviceJson, term.env, newValue)
        const newConfig = parseTrafficAnalysisResult(newDeviceJson)
        
        await client.disconnect()
        client.setDevice(newConfig.device)
        const reset = await client.reset(); steplog("reset: " + reset)

        const connect = await client.connect(); 
        if(connect === "connack") steplog("connect: " + connect)
        else {
            steplog("connect: " + connect, true)
            reportRejected(term, "connect", connect, newValue)
            break;
        }

        const publish = await client.publish(); 
        if(publish === "success") steplog("publish: " + publish)
        else {
            steplog("publish: " + publish, true)
            reportRejected(term, "publish", publish, newValue)
            break;
        }

        const subscribe = await client.subscribe()
        if(subscribe === "success") steplog("subscribe: " + subscribe)
        else {
            steplog("subscribe: " + subscribe, true)
            reportRejected(term, "subscribe", subscribe, newValue)
            break;
        }

        const unsubscribe = await client.unsubscribe()
        if(unsubscribe === "success") steplog("unsubscribe: " + unsubscribe)
        else {
            steplog("unsubscribe: " + unsubscribe, true)
            reportRejected(term, "unsubscribe", unsubscribe, newValue)
            break;
        }


        const disconnect = await client.disconnect()
        if(disconnect === "closed") steplog("disconnect: " + disconnect)
        else {
            steplog("disconnect: " + disconnect, true)
            reportRejected(term, "disconnect", disconnect, newValue)
            break;
        }
        
        reportRejected(term, "", "", newValue, true)
    }

    config.device = originalDevice
})

app.get('/mqtt/initClient', async (req, res, next) => {
    client = new MyMqttClient
    const dev = LoadClientFromConfig(req.query.filename )
    client.setDevice(dev)
    res.send('OK')
})

app.get('/mqtt/connect', async (req, res, next) => { 
    // console.log("connect")
    const success = await client.connect()
    res.send(success)
})

app.get('/mqtt/disconnect', async (req, res, next) => { 
    const success = await client.disconnect()
    res.send(success)
})

app.get('/mqtt/publish', async (req, res, next) => { 
    const success = await client.publish()
    res.send(success)
})

app.get('/mqtt/subscribe', async (req, res, next) => { 
    const success = await client.subscribe()
    res.send(success)
})

app.get('/mqtt/unsubscribe', async (req, res, next) => { 
    const success = await client.unsubscribe()
    res.send(success)
})

app.get('/mqtt/reset', async (req, res, next) => { 
    const success = await client.reset()
    res.send(success)
})

app.get('/mqtt/savelog', async (req, res, next) => { 
    const success = await client.savelog()
    res.send(success)
})

app.listen(port, () => console.log(`Open link in browser: 127.0.0.1:${port}`))

