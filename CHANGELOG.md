# 变更记录

## 发行版本
### v1.2.0 - 2021-11-28
- :sparkles: mqtt-mqtt-core client IMqttClientConnectListener 添加 onDisconnect 方法。gitee #I4JT1D 感谢 `@willianfu` 同学反馈。
- :sparkles: mica-mqtt-core server IMqttMessageListener 接口调整，不兼容老版本。
- :sparkles: mica-mqtt-broker 调整上下行消息通道。
- :sparkles: mica-mqtt-broker 添加节点管理。
- :sparkles: mica-mqtt-broker 调整默认的 Message 序列化方式，不兼容老版本。
- :sparkles: mica-mqtt-broker 优化设备上下线，处理节点停机的情况。
- :sparkles: 抽取 mica-mqtt-model 模块，方便后期支持消息桥接，Message 添加默认的消息序列化。 gitee #I4ECEO
- :sparkles: mica-mqtt-model 完善 Message 消息模型，方便集群。
- :bug: mica-mqtt-core MqttClient 修复 ssl 没有设置。
- :memo: 完善 mica-mqtt-broker README.md，添加二开说明。
- :memo: 统一 mica-mqtt server ip 文档。
- :memo: 更新 README.md
- :arrow_up: 升级 tio 到 3.7.5.v20211028-RELEASE AioDecodeException 改为 TioDecodeException，

### v1.1.4 - 2021-10-16
- :sparkles: 添加 uniqueId 概念，用来处理 clientId 不唯一的场景。详见：gitee #I4DXQU
- :sparkles: 微调 `IMqttServerAuthHandler` 认证，添加 uniqueId 参数。

### v1.1.3 - 2021-10-13
- :sparkles: 状态事件接口 `IMqttConnectStatusListener` 添加 ChannelContext 参数。
- :sparkles: 从认证中拆分 `IMqttServerSubscribeValidator` 订阅校验接口，添加 ChannelContext、clientId 参数。
- :sparkles: 认证 `IMqttServerAuthHandler` 调整包、添加 ChannelContext 参数。
- :sparkles: 完善文档和示例，添加默认端口号说明。
- :arrow_up: 依赖升级

### v1.1.2 - 2021-09-12
- :sparkles: 添加 mica-mqtt-broker 模块，基于 redis pub/sub 实现 mqtt 集群。
- :sparkles: mica-mqtt-broker 基于 redis 实现客户端状态存储。
- :sparkles: mica-mqtt-broker 基于 redis 实现遗嘱、保留消息存储。
- :sparkles: mqtt-server http api 调整订阅和取消订阅，方便集群处理。
- :sparkles: mica-mqtt-spring-boot-example 添加 mqtt 和 http api 认证示例。
- :sparkles: 添加 mqtt 5 所有 ReasonCode。
- :sparkles: 优化解码 PacketNeededLength 计算。
- :bug: 修复遗嘱消息，添加消息类型。
- :bug: 修复 mqtt-server 保留消息匹配规则。

### v1.1.1 - 2021-09-05
- :sparkles: mqtt-server 优化连接关闭日志。
- :sparkles: mqtt-server 优化订阅，相同 topicFilter 订阅对 qos 判断。
- :sparkles: mqtt-server 监听器添加 try catch，避免因业务问题导致连接断开。
- :sparkles: mqtt-server 优化 topicFilters 校验。
- :sparkles: mqtt-client 优化订阅 reasonCodes 判断。
- :sparkles: mqtt-client 监听器添加 try catch，避免因业务问题导致连接断开。
- :sparkles: mqtt-client 添加 session 有效期。
- :sparkles: 代码优化，减少 codacy 上的问题。
- :bug: mqtt-server 修复心跳时间问题。
- :bug: 修复 mqtt-server 多个订阅同时匹配时消息重复的问题。
- :bug: mqtt-client 优化连接处理的逻辑，mqtt 连接之后再订阅。
- :bug: 修复 MqttProperties 潜在的一个空指针。

### v1.1.0 - 2021-08-29
- :sparkles: 重构，内置 http，http 和 websocket 公用端口。
- :sparkles: 添加 mica-core 中的 HexUtil。
- :sparkles: 添加 PayloadEncode 工具。
- :sparkles: ServerTioConfig#share 方法添加 groupStat。
- :sparkles: 考虑使用 udp 多播做集群。
- :sparkles: MqttServer、MqttServerTemplate 添加 close、getChannelContext 等方法。
- :sparkles: 重构 MqttServerConfiguration 简化代码。
- :sparkles: 配置项 `mqtt.server.websocket-port` 改为 `mqtt.server.web-port`。
- :memo: 添加 JetBrains 连接。
- :bug: 修复默认的消息转发器逻辑。
- :bug: 修复 websocket 下线无法触发offline gitee #I47K13 感谢 `@willianfu` 同学反馈。 

### v1.0.6 - 2021-08-21
- :sparkles: 添加订阅 topicFilter 校验。
- :sparkles: 优化压测工具，更新压测说明，添加 tcp 连接数更改文档地址。
- :sparkles: mica-mqtt-example 添加多设备交互示例。
- :sparkles: 优化 mica-mqtt-spring-boot-example。
- :sparkles: 优化 deploy.sh 脚本。
- :bug: 优化解码异常处理。
- :bug: 修复心跳超时处理。
- :arrow_up: 升级 spring boot 到 2.5.4

### v1.0.5 - 2021-08-15
- :bug: 修复编译导致的 java8 运行期间的部分问题，NoSuchMethodError: java.nio.ByteBuffer.xxx

### v1.0.3 - 2021-08-15
- :sparkles: mica-mqtt server 添加 websocket mqtt 子协议支持（支持 mqtt.js）。
- :sparkles: mica-mqtt server ip，默认为空，可不设置。
- :sparkles: mica-mqtt client去除 CountDownLatch 避免启动时未连接上服务端卡住。
- :sparkles: mica-mqtt client 添加最大包体长度字段，避免超过 8092 长度的包体导致解析异常。
- :sparkles: mica-mqtt client 添加连接监听 IMqttClientConnectListener。
- :sparkles: mica-mqtt 3.1 协议会校验 clientId 长度，添加配置项 maxClientIdLength。
- :sparkles: mica-mqtt 优化 mqtt 解码异常处理。
- :sparkles: mica-mqtt 日志优化，方便查询。
- :sparkles: mica-mqtt 代码优化，部分 Tio.close 改为 Tio.remove。
- :sparkles: mica-mqtt-spring-boot-example 添加 Dockerfile，支持 `spring-boot:build-image`。
- :sparkles: 完善 mica-mqtt-spring-boot-starter，添加遗嘱消息配置。
- :arrow_up: 升级 t-io 到 3.7.4。

### v1.0.3-RC - 2021-08-12
- :sparkles: 添加 websocket mqtt 子协议支持（支持 mqtt.js）。
- :sparkles: mqtt 客户端去除 CountDownLatch 避免启动时未连接上服务端卡住。
- :sparkles: mica-mqtt 服务端 ip，默认为空，可不设置。
- :sparkles: 完善 mica-mqtt-spring-boot-starter，添加遗嘱消息配置。
- :sparkles: mqtt 3.1 协议会校验 clientId 长度，添加设置。
- :sparkles: mqtt 日志优化，方便查询。
- :sparkles: 代码优化，部分 Tio.close 改为 Tio.remove。
- :arrow_up: 升级 t-io 到 3.7.4。

### v1.0.2 - 2021-08-08
- :memo: 文档添加集群处理步骤说明，添加遗嘱消息、保留消息的使用场景。
- :sparkles: 去除演示中的 qos2 参数，性能损耗大避免误用。
- :sparkles: 遗嘱、保留消息内部消息转发抽象。
- :sparkles: mqtt server 连接时先判断 clientId 是否存在连接关系，有则先关闭已有连接。
- :sparkles: 添加 mica-mqtt-spring-boot-example 。感谢 wsq（ @冷月宫主 ）pr。
- :sparkles: mica-mqtt-spring-boot-starter 支持客户端接入和服务端优化。感谢 wsq（ @冷月宫主 ）pr。
- :sparkles: mica-mqtt-spring-boot-starter 服务端支持指标收集。可对接 `Prometheus + Grafana` 监控。
- :sparkles: mqtt server 接受连接时，先判断该 clientId 是否存在其它连接，有则解绑并关闭其他连接。
- :arrow_up: 升级 mica-auto 到 2.1.3 修复 ide 多模块增量编译问题。

### v1.0.2-RC - 2021-08-04
- :sparkles: 添加 mica-mqtt-spring-boot-example 。感谢 wsq（ @冷月宫主 ）pr。
- :sparkles: mica-mqtt-spring-boot-starter 支持客户端接入和服务端优化。感谢 wsq（ @冷月宫主 ）pr。
- :sparkles: mica-mqtt-spring-boot-starter 服务端支持指标收集。可对接 `Prometheus + Grafana` 监控。
- :sparkles: mqtt server 接受连接时，先判断该 clientId 是否存在其它连接，有则解绑并关闭其他连接。

### v1.0.1 - 2021-08-02
- :sparkles: 订阅管理集成到 session 管理中。
- :sparkles: MqttProperties.MqttPropertyType 添加注释，考虑 mqtt V5.0 新特性处理。
- :sparkles: 添加 Spring boot starter 方便接入，兼容低版本 Spring boot。
- :sparkles: 调研 t-io websocket 子协议。
- :bug: 修复 java8 运行期间的部分问题，NoSuchMethodError: java.nio.ByteBuffer.xxx

### v1.0.1-RC - 2021-07-31
- :sparkles: 添加 Spring boot starter 方便接入。
- :sparkles: 调研 t-io websocket 子协议。

### v1.0.0 - 2021-07-29
- :sparkles: 基于低延迟高性能的 t-io AIO 框架。
- :sparkles: 支持 MQTT v3.1、v3.1.1 以及 v5.0 协议。
- :sparkles: 支持 MQTT client 客户端。
- :sparkles: 支持 MQTT server 服务端。
- :sparkles: 支持 MQTT 遗嘱消息。
- :sparkles: 支持 MQTT 保留消息。
- :sparkles: 支持自定义消息（mq）处理转发实现集群。
- :sparkles: 支持 GraalVM 编译成本机可执行程序。