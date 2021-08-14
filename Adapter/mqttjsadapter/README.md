# JS HTTP API 说明

## 加载 Sematics 文件

- api: `/mqtt/config/fromTrafficAnalysis`
- 方式: post
- 请求格式:

```json
{
    "data0": "result.json 的内容",
    "data1": "raw.json 的内容",
    "args": {
        "userFilledTerms.*": "用户需要输入的数据",
        "parsingOptions.": "parse时候的选项",
        "debug_result_yaml_save_path": "将返回值输出到 yaml 文件里"
    }
}
```

### `userFilledTerms.*` 说明

这个选项是用户需要额外输入的数据，这包括了:

| 名字 | 说明 |
|------|------|
|alitcp.password.key | alitcp password 字段试用的密钥 |
|gcp.jwt.key | gcp jwt 的密钥 |
|tuya.publish.key| tuya publish 的 payload 加密需要的localKey |
| protocol | 协议, `mqtt` 或者 `mqtts`, 默认 mqtt |
| secureProtocol | 如果protocol选择mqtts, 那么需要指定加密方式, 常用的如 `TLSv1_2_method` |
| host | **必填**, mqtt 服务器地址 |
| port | **必填**, mqtt 服务器端口 |

比如要指定  alitcp 的密钥，我们可以这样定义 "args"

```json
{
    "data0": "result.json 的内容",
    "data1": "raw.json 的内容",
    "args": {
        "userFilledTerms.alitcp.password.key": "some key",
        "userFilledTerms.host": "xxx.alibaba.com",
        "userFilledTerms.port": 1888
    }
}
```

### `parsingOptions.*` 说明

| 名字 | 值类型 |  说明 |
|------|-------|------|
| enforcePasswordUseRaw | bool | 强制指定密码使用固定值, 忽略jwt等加密手段 |
| passwordRaw | string | 密码的固定值 |



## 运行分析工具
- api: `/mqtt/config/analyze/fields`
- 方式: get
- 参数: 无

- 返回格式举例说明:

```json
[
    {
        "field": {                 // 字段信息
            "env": [               // 字段在输入 json 文件的位置 
                "CONNECT",         
                "password"
            ],
            "val": "mosq_dummy_password", // 这个字段原来的值
            "encrypted": true       // 这个字段是否被加密
        },
        "newValue": "mosq_dummy_passwore",  // 修改后的值
        "replayable": true,   // 是否可以重放
        "modifiable": false,  // 是否可以修改
        "stage": "connect",   // 这个字段在 connect/publish 等阶段被使用
        "reason": "error: Error: Connection refused: Not authorized" // 重放失败/修改消息失败的原因
    },
    {
        "field": {
            "env": [
                "CONNECT",
                "username"
            ],
            "val": "Unit02&a1EQiFSQ6Js",
            "encrypted": false   // 这是一个不加密的字段
        },
        "newValue": "Unit02&a1EQiFSQ6Jr",
        "replayable": true,
        "modifiable": true,
        "reason": ""
    },
    ...
]
```





# Mosquitto 服务端

进入 `mosquitto/` 文件夹，运行命令启动服务端。

```bash
./mosquitto.exe -c ./configure/mosquitto.conf -p 9091 -v
```

要修改服务端验证能力（比如增加jwt验证等），请直接修改 [`mosquitto/configure/ap_dummy.c`](./mosquitto/configure/ap_dummy.c), 然后用 cmake 编译。

## mosquitto.conf 文件

在 mosquitto/configure/mosquitto.conf 文件里可以修改 auth 模块的行为

```c

// 接受任何 password
auth_opt_password_auth none

// 只接受 password = 'mosq_dummy_password'
auth_opt_password_auth match

```


# 调用流程说明

1. 首先传入 result.json 和 raw.json 参数。对于 Java 来说，调用 `MQTTJSMapper.loadFromTrafficAnalysis(result, raw, args)` 函数即可。其中 `args` 是一个 Map。

针对 mosquitto 服务端，我们可以使用 alitcp 作为 result.json 和 raw.json, 然后传入以下参数:

``` json
// Map args 的内容
{
    // 强制使用 mosquitto auth 默认的密码
    "parsingOptions.enforcePasswordUseRaw": true,

    // mosquitto auth 模块 默认密钥
    "parsingOptions.passwordRaw": "mosq_dummy_password", 

    // mosquitto 服务器
    "userFilledTerms.host": "127.0.0.1",
    "userFilledTerms.port": 9091,

    // 保存解析 result.json 和 raw.json 的结果。
    "debug_result_yaml_save_path": "cache/mos.yml"
}

```

2. 直接调用 `/mqtt/config/analyze/fields`, 不需要参数，获得每个字段变更服务器是否接受的结果。