

const fs = require('fs');
const mqtt = require('mqtt');
const mqttPacket = require('mqtt-packet');
const LoadClientFromConfig = require('./config')().LoadClientFromConfig //loadconfig
const express = require('express')
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
        console.log(device.properties);
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
                this.resolve_fn('error')
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
                if(this.closed) {
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
                this.client.subscribe(sub.topic, {properties: sub.properties, qos: sub.qos, enforceSend: true}, (error)=>{
                    
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


// for test
app.get('/', (req, res)=>{
    //res.send(fs.readFileSync("index.html"))
    res.sendFile("index.html", { root: __dirname })
})

app.get('/mqtt/initClient', async (req, res, next) => {
    client = new MyMqttClient
    const dev = LoadClientFromConfig(req.query.filename )
    client.setDevice(dev)
    res.send('OK')
})

app.get('/mqtt/connect', async (req, res, next) => { 
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

