/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.iot.mqtt.core.server.support;

import net.dreamlu.iot.mqtt.codec.*;
import net.dreamlu.iot.mqtt.core.common.MqttPendingPublish;
import net.dreamlu.iot.mqtt.core.common.MqttPendingQos2Publish;
import net.dreamlu.iot.mqtt.core.server.MqttConst;
import net.dreamlu.iot.mqtt.core.server.MqttServerCreator;
import net.dreamlu.iot.mqtt.core.server.MqttServerProcessor;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerAuthHandler;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerUniqueIdService;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.event.IMqttConnectStatusListener;
import net.dreamlu.iot.mqtt.core.server.event.IMqttMessageListener;
import net.dreamlu.iot.mqtt.core.server.model.Message;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.utils.hutool.StrUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * mqtt broker 处理器
 *
 * @author L.cm
 */
public class DefaultMqttServerProcessor implements MqttServerProcessor {
	private static final Logger logger = LoggerFactory.getLogger(DefaultMqttServerProcessor.class);
	/**
	 * 2 倍客户端 keepAlive 时间
	 */
	private static final long KEEP_ALIVE_UNIT = 2000L;
	private final long heartbeatTimeout;
	private final IMqttMessageStore messageStore;
	private final IMqttSessionManager sessionManager;
	private final IMqttServerAuthHandler authHandler;
	private final IMqttServerUniqueIdService uniqueIdService;
	private final IMqttServerSubscribeValidator subscribeValidator;
	private final IMqttMessageDispatcher messageDispatcher;
	private final IMqttConnectStatusListener connectStatusListener;
	private final IMqttMessageListener messageListener;
	private final ScheduledThreadPoolExecutor executor;

	public DefaultMqttServerProcessor(MqttServerCreator serverCreator, ScheduledThreadPoolExecutor executor) {
		this.heartbeatTimeout = serverCreator.getHeartbeatTimeout() == null ? 120_000L : serverCreator.getHeartbeatTimeout();
		this.messageStore = serverCreator.getMessageStore();
		this.sessionManager = serverCreator.getSessionManager();
		this.authHandler = serverCreator.getAuthHandler();
		this.uniqueIdService = serverCreator.getUniqueIdService();
		this.subscribeValidator = serverCreator.getSubscribeValidator();
		this.messageDispatcher = serverCreator.getMessageDispatcher();
		this.connectStatusListener = serverCreator.getConnectStatusListener();
		this.messageListener = serverCreator.getMessageListener();
		this.executor = executor;
	}

	@Override
	public void processConnect(ChannelContext context, MqttConnectMessage mqttMessage) {
		MqttConnectPayload payload = mqttMessage.payload();
		// 参数
		String clientId = payload.clientIdentifier();
		String userName = payload.userName();
		String password = payload.password();
		// 1. 获取唯一id，用于 mqtt 内部绑定，部分用户的业务采用 userName 作为唯一id，故抽象之，默认：uniqueId == clientId
		String uniqueId = uniqueIdService.getUniqueId(context, clientId, userName, password);
		// 2. 客户端必须提供 uniqueId, 不管 cleanSession 是否为1, 此处没有参考标准协议实现
		if (StrUtil.isBlank(uniqueId)) {
			connAckByReturnCode(clientId, uniqueId, context, MqttConnectReasonCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
			return;
		}
		// 3. 认证
		if (!authHandler.authenticate(context, uniqueId, clientId, userName, password)) {
			connAckByReturnCode(clientId, uniqueId, context, MqttConnectReasonCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
			return;
		}
		// 4. 判断 uniqueId 是否在多个地方使用，如果在其他地方有使用，先解绑
		ChannelContext otherContext = Tio.getByBsId(context.getTioConfig(), uniqueId);
		if (otherContext != null) {
			Tio.unbindBsId(otherContext);
			String remark = String.format("uniqueId:[%s] clientId:[%s] now bind on new context id:[%s]", uniqueId, clientId, context.getId());
			Tio.remove(otherContext, remark);
		}
		// 5. 绑定 uniqueId
		Tio.bindBsId(context, uniqueId);
		MqttConnectVariableHeader variableHeader = mqttMessage.variableHeader();
		// 6. 心跳超时时间，当然这个值如果小于全局配置（默认：120s），定时检查的时间间隔还是以全局为准，只是在判断时用此值
		int keepAliveSeconds = variableHeader.keepAliveTimeSeconds();
		// 2倍客户端 keepAlive 时间作为服务端心跳超时时间，如果配置同全局默认不设置，节约内存
		if (keepAliveSeconds > 0 && heartbeatTimeout != keepAliveSeconds * KEEP_ALIVE_UNIT) {
			context.setHeartbeatTimeout(keepAliveSeconds * KEEP_ALIVE_UNIT);
		}
		// 7. session 处理，先默认全部连接关闭时清除
//		boolean cleanSession = variableHeader.isCleanSession();
//		if (cleanSession) {
//			// TODO L.cm 考虑 session 处理 可参数： https://www.emqx.com/zh/blog/mqtt-session
//			// mqtt v5.0 会话超时时间
//			MqttProperties properties = variableHeader.properties();
//			Integer sessionExpiryInterval = properties.getPropertyValue(MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL);
//			System.out.println(sessionExpiryInterval);
//		}
		// 8. 存储遗嘱消息
		boolean willFlag = variableHeader.isWillFlag();
		if (willFlag) {
			Message willMessage = new Message();
			willMessage.setMessageType(MqttMessageType.PUBLISH.value());
			willMessage.setFormClientId(uniqueId);
			willMessage.setTopic(payload.willTopic());
			willMessage.setPayload(payload.willMessageInBytes());
			willMessage.setQos(variableHeader.willQos());
			willMessage.setRetain(variableHeader.isWillRetain());
			messageStore.addWillMessage(uniqueId, willMessage);
		}
		// 9. 返回 ack
		connAckByReturnCode(clientId, uniqueId, context, MqttConnectReasonCode.CONNECTION_ACCEPTED);
		// 10. 在线状态
		connectStatusListener.online(context, uniqueId);
	}

	private void connAckByReturnCode(String clientId, String uniqueId, ChannelContext context, MqttConnectReasonCode returnCode) {
		MqttConnAckMessage message = MqttMessageBuilders.connAck()
			.returnCode(returnCode)
			.sessionPresent(false)
			.build();
		Tio.send(context, message);
		logger.info("Connect ack send - clientId: {} uniqueId:{} returnCode:{}", clientId, uniqueId, returnCode);
	}

	@Override
	public void processPublish(ChannelContext context, MqttPublishMessage message) {
		String clientId = context.getBsId();
		MqttFixedHeader fixedHeader = message.fixedHeader();
		MqttQoS mqttQoS = fixedHeader.qosLevel();
		MqttPublishVariableHeader variableHeader = message.variableHeader();
		String topicName = variableHeader.topicName();
		int packetId = variableHeader.packetId();
		logger.debug("Publish - clientId:{} topicName:{} mqttQoS:{} packetId:{}", clientId, topicName, mqttQoS, packetId);
		switch (mqttQoS) {
			case AT_MOST_ONCE:
				invokeListenerForPublish(clientId, mqttQoS, topicName, message);
				break;
			case AT_LEAST_ONCE:
				invokeListenerForPublish(clientId, mqttQoS, topicName, message);
				if (packetId != -1) {
					MqttMessage messageAck = MqttMessageBuilders.pubAck()
						.packetId(packetId)
						.build();
					Boolean resultPubAck = Tio.send(context, messageAck);
					logger.debug("Publish - PubAck send clientId:{} topicName:{} mqttQoS:{} packetId:{} result:{}", clientId, topicName, mqttQoS, packetId, resultPubAck);
				}
				break;
			case EXACTLY_ONCE:
				if (packetId != -1) {
					MqttFixedHeader pubRecFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
					MqttMessage pubRecMessage = new MqttMessage(pubRecFixedHeader, MqttMessageIdVariableHeader.from(packetId));
					MqttPendingQos2Publish pendingQos2Publish = new MqttPendingQos2Publish(message, pubRecMessage);
					Boolean resultPubRec = Tio.send(context, pubRecMessage);
					logger.debug("Publish - PubRec send clientId:{} topicName:{} mqttQoS:{} packetId:{} result:{}", clientId, topicName, mqttQoS, packetId, resultPubRec);
					sessionManager.addPendingQos2Publish(clientId, packetId, pendingQos2Publish);
					pendingQos2Publish.startPubRecRetransmitTimer(executor, msg -> Tio.send(context, msg));
				}
				break;
			case FAILURE:
			default:
				break;
		}
	}

	@Override
	public void processPubAck(ChannelContext context, MqttMessageIdVariableHeader variableHeader) {
		int messageId = variableHeader.messageId();
		String clientId = context.getBsId();
		logger.debug("PubAck - clientId:{}, messageId:{}", clientId, messageId);
		MqttPendingPublish pendingPublish = sessionManager.getPendingPublish(clientId, messageId);
		if (pendingPublish == null) {
			return;
		}
		pendingPublish.onPubAckReceived();
		sessionManager.removePendingPublish(clientId, messageId);
		pendingPublish.getPayload().clear();
	}

	@Override
	public void processPubRec(ChannelContext context, MqttMessageIdVariableHeader variableHeader) {
		String clientId = context.getBsId();
		int messageId = variableHeader.messageId();
		logger.debug("PubRec - clientId:{}, messageId:{}", clientId, messageId);
		MqttPendingPublish pendingPublish = sessionManager.getPendingPublish(clientId, messageId);
		if (pendingPublish == null) {
			return;
		}
		pendingPublish.onPubAckReceived();

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
		MqttMessage pubRelMessage = new MqttMessage(fixedHeader, variableHeader);
		Tio.send(context, pubRelMessage);

		pendingPublish.setPubRelMessage(pubRelMessage);
		pendingPublish.startPubRelRetransmissionTimer(executor, msg -> Tio.send(context, msg));
	}

	@Override
	public void processPubRel(ChannelContext context, MqttMessageIdVariableHeader variableHeader) {
		String clientId = context.getBsId();
		int messageId = variableHeader.messageId();
		logger.debug("PubRel - clientId:{}, messageId:{}", clientId, messageId);
		MqttPendingQos2Publish pendingQos2Publish = sessionManager.getPendingQos2Publish(clientId, messageId);
		if (pendingQos2Publish != null) {
			MqttPublishMessage incomingPublish = pendingQos2Publish.getIncomingPublish();
			String topicName = incomingPublish.variableHeader().topicName();
			MqttFixedHeader incomingFixedHeader = incomingPublish.fixedHeader();
			MqttQoS mqttQoS = incomingFixedHeader.qosLevel();
			invokeListenerForPublish(clientId, mqttQoS, topicName, incomingPublish);
			pendingQos2Publish.onPubRelReceived();
			sessionManager.removePendingQos2Publish(clientId, messageId);
		}
		MqttMessage message = MqttMessageFactory.newMessage(
			new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0),
			MqttMessageIdVariableHeader.from(messageId), null);
		Tio.send(context, message);
	}

	@Override
	public void processPubComp(ChannelContext context, MqttMessageIdVariableHeader variableHeader) {
		int messageId = variableHeader.messageId();
		String clientId = context.getBsId();
		logger.debug("PubComp - clientId:{}, messageId:{}", clientId, messageId);
		MqttPendingPublish pendingPublish = sessionManager.getPendingPublish(clientId, messageId);
		if (pendingPublish != null) {
			pendingPublish.getPayload().clear();
			pendingPublish.onPubCompReceived();
			sessionManager.removePendingPublish(clientId, messageId);
		}
	}

	@Override
	public void processSubscribe(ChannelContext context, MqttSubscribeMessage message) {
		String clientId = context.getBsId();
		int messageId = message.variableHeader().messageId();
		// 1. 校验订阅的 topicFilter
		List<MqttTopicSubscription> topicSubscriptions = message.payload().topicSubscriptions();
		if (subscribeValidator != null && !subscribeValidator.isValid(context, clientId, topicSubscriptions)) {
			logger.error("Subscribe - clientId:{} topicFilters:{} verification failed messageId:{}", clientId, topicSubscriptions, messageId);
			// 3. 返回 ack
			MqttMessage subAckMessage = MqttMessageBuilders.subAck()
				.addGrantedQos(MqttQoS.FAILURE)
				.packetId(messageId)
				.build();
			Tio.send(context, subAckMessage);
			return;
		}
		// 2. 存储 clientId 订阅的 topic
		List<MqttQoS> mqttQosList = new ArrayList<>();
		List<String> topicList = new ArrayList<>();
		for (MqttTopicSubscription subscription : topicSubscriptions) {
			String topicName = subscription.topicName();
			MqttQoS mqttQoS = subscription.qualityOfService();
			mqttQosList.add(mqttQoS);
			topicList.add(topicName);
			sessionManager.addSubscribe(topicName, clientId, mqttQoS.value());
		}
		logger.info("Subscribe - clientId:{} TopicFilters:{} mqttQoS:{} messageId:{}", clientId, topicList, mqttQosList, messageId);
		// 3. 返回 ack
		MqttMessage subAckMessage = MqttMessageBuilders.subAck()
			.addGrantedQosList(mqttQosList)
			.packetId(messageId)
			.build();
		Tio.send(context, subAckMessage);
		// 4. 发送保留消息
		for (String topic : topicList) {
			List<Message> retainMessageList = messageStore.getRetainMessage(topic);
			if (retainMessageList != null && !retainMessageList.isEmpty()) {
				for (Message retainMessage : retainMessageList) {
					messageDispatcher.send(clientId, retainMessage);
				}
			}
		}
	}

	@Override
	public void processUnSubscribe(ChannelContext context, MqttUnsubscribeMessage message) {
		String clientId = context.getBsId();
		int messageId = message.variableHeader().messageId();
		List<String> topicFilterList = message.payload().topics();
		for (String topicFilter : topicFilterList) {
			sessionManager.removeSubscribe(topicFilter, clientId);
		}
		logger.info("UnSubscribe - clientId:{} Topic:{} messageId:{}", clientId, topicFilterList, messageId);
		MqttMessage unSubMessage = MqttMessageBuilders.unsubAck()
			.packetId(messageId)
			.build();
		Tio.send(context, unSubMessage);
	}

	@Override
	public void processPingReq(ChannelContext context) {
		String clientId = context.getBsId();
		logger.debug("PingReq - clientId:{}", clientId);
		Tio.send(context, MqttMessage.PINGRESP);
	}

	@Override
	public void processDisConnect(ChannelContext context) {
		String clientId = context.getBsId();
		logger.info("DisConnect - clientId:{} contextId:{}", clientId, context.getId());
		// 设置正常断开的标识
		context.set(MqttConst.DIS_CONNECTED, (byte) 1);
		Tio.remove(context, "Mqtt DisConnect");
	}

	/**
	 * 处理订阅的消息
	 *
	 * @param clientId  clientId
	 * @param topicName topicName
	 * @param message   MqttPublishMessage
	 */
	private void invokeListenerForPublish(String clientId, MqttQoS mqttQoS, String topicName, MqttPublishMessage message) {
		MqttFixedHeader fixedHeader = message.fixedHeader();
		boolean isRetain = fixedHeader.isRetain();
		ByteBuffer payload = message.payload();
		// 1. retain 消息逻辑
		if (isRetain) {
			// qos == 0 or payload is none,then clear previous retain message
			if (MqttQoS.AT_MOST_ONCE == mqttQoS || payload == null || payload.array().length == 0) {
				this.messageStore.clearRetainMessage(topicName);
			} else {
				Message retainMessage = new Message();
				retainMessage.setTopic(topicName);
				retainMessage.setQos(mqttQoS.value());
				retainMessage.setPayload(payload.array());
				retainMessage.setFormClientId(clientId);
				retainMessage.setMessageType(MqttMessageType.PUBLISH.value());
				retainMessage.setRetain(true);
				retainMessage.setDup(fixedHeader.isDup());
				retainMessage.setTimestamp(System.currentTimeMillis());
				this.messageStore.addRetainMessage(topicName, retainMessage);
			}
		}
		// 2. 消息发布
		try {
			messageListener.onMessage(clientId, message);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}

}
