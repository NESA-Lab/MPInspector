gcp1-1 sub topic 不被允许
gcp1-2 sub 的时候有来自 /devices/dev5/config 消息，这是在之前实验中配置的
gcp3-2 开始 sub 另一个主题, 之前/devices/dev7/config只要sub了服务器一定会发消息, 哪怕是空的服务器也会pub给设备, 但是 /devices/dev7/commands/# 不一定会发
gcp4-2 开始 pub 另一个主题, 原先是 /devices/dev8/state, 改成 /devices/dev8/events/temp
gcp5-2 pub/sub 都用 QoS 0, 之前一概用1, 但是服务器 sub granted qos 一直是 0
*gcp1-3有点特殊, 其他设备都是用同一个密钥(不妨称为密钥A)生成 JWT, 但是它这次连接用另一个密钥(不妨称为密钥B)。一个设备最多有三个密钥。gcp1-1, gcp1-2 用的和其他设备一样的密钥(A)
有趣的是, 我本来想用旧的密钥, 就是密钥B, 但是 GCP 拒绝我上传的密钥B的公钥, 他说这个证书已经过期, 虽然密钥B不能添加作为其他设备的密钥, 但是这个密钥还是可以用来连接之前绑定这个密钥的设备。