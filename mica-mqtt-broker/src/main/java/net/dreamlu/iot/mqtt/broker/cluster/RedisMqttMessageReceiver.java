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

package net.dreamlu.iot.mqtt.broker.cluster;

import net.dreamlu.iot.mqtt.codec.MqttMessageType;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.model.Message;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.mica.core.utils.JsonUtil;
import net.dreamlu.mica.core.utils.StringUtil;
import net.dreamlu.mica.redis.cache.MicaRedisCache;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.tio.core.ChannelContext;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * 监听集群消息
 *
 * @author L.cm
 */
public class RedisMqttMessageReceiver implements MessageListener, InitializingBean {
	private final RedisTemplate<String, Object> redisTemplate;
	private final String channel;
	private final MqttServer mqttServer;
	private final IMqttSessionManager sessionManager;

	public RedisMqttMessageReceiver(MicaRedisCache redisCache,
									String channel,
									MqttServer mqttServer) {
		this.redisTemplate = redisCache.getRedisTemplate();
		this.channel = Objects.requireNonNull(channel, "Redis pub/sub channel is null.");
		this.mqttServer = mqttServer;
		this.sessionManager = mqttServer.getServerCreator().getSessionManager();
	}

	@Override
	public void onMessage(org.springframework.data.redis.connection.Message message, byte[] bytes) {
		byte[] messageBody = message.getBody();
		// 手动序列化和反序列化，避免 redis 序列化不一致问题
		Message mqttMessage = JsonUtil.readValue(messageBody, Message.class);
		if (mqttMessage == null) {
			return;
		}
		messageProcessing(mqttMessage);
	}

	public void messageProcessing(Message message) {
		MqttMessageType messageType = MqttMessageType.valueOf(message.getMessageType());
		String topic = message.getTopic();
		if (MqttMessageType.PUBLISH == messageType) {
			MqttQoS mqttQoS = MqttQoS.valueOf(message.getQos());
			boolean retain = message.isRetain();
			// 消息需要发送到的客户端
			String clientId = message.getToClientId();
			// TODO L.cm 待添加处理逻辑 https://gitee.com/596392912/mica-mqtt/issues/I4ECEO
			if (StringUtil.isBlank(clientId)) {
				mqttServer.publishAll(topic, ByteBuffer.wrap(message.getPayload()), mqttQoS, retain);
			} else {
				mqttServer.publish(clientId, topic, ByteBuffer.wrap(message.getPayload()), mqttQoS, retain);
			}
		} else if (MqttMessageType.SUBSCRIBE == messageType) {
			String formClientId = message.getFormClientId();
			ChannelContext context = mqttServer.getChannelContext(formClientId);
			if (context != null) {
				sessionManager.addSubscribe(topic, formClientId, message.getQos());
			}
		} else if (MqttMessageType.UNSUBSCRIBE == messageType) {
			String formClientId = message.getFormClientId();
			ChannelContext context = mqttServer.getChannelContext(formClientId);
			if (context != null) {
				sessionManager.removeSubscribe(topic, formClientId);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		byte[] channelBytes = RedisSerializer.string().serialize(channel);
		redisTemplate.execute((RedisCallback<Void>) connection -> {
			connection.subscribe(RedisMqttMessageReceiver.this, channelBytes);
			return null;
		});
	}
}
