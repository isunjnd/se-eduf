/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.net.dreamlu.net).
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

package net.dreamlu.iot.mqtt.core.client;

import net.dreamlu.iot.mqtt.codec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;

import java.nio.ByteBuffer;

/**
 * 默认的 mqtt 消息处理器
 *
 * @author L.cm
 */
public class DefaultMqttClientProcessor implements MqttClientProcessor {
	private static final Logger logger = LoggerFactory.getLogger(DefaultMqttClientProcessor.class);
	private final MqttClientSubManage subManage;

	public DefaultMqttClientProcessor(MqttClientSubManage subManage) {
		this.subManage = subManage;
	}

	@Override
	public void processConAck(ChannelContext context, MqttConnAckMessage message) {
		MqttConnectReturnCode returnCode = message.variableHeader().connectReturnCode();
		switch (message.variableHeader().connectReturnCode()) {
			case CONNECTION_ACCEPTED:
				logger.info("MQTT 连接成功！");
				break;
			case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
			case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
			case CONNECTION_REFUSED_NOT_AUTHORIZED:
			case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
			case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
			default:
				Tio.close(context, "MqttClient connect error.");
				context.setClosed(true);
				throw new IllegalStateException("MqttClient connect error ReturnCode:" + returnCode);
		}
	}

	@Override
	public void processSubAck(MqttSubAckMessage message) {
		System.out.println(message);
	}

	@Override
	public void processPublish(ChannelContext context, MqttPublishMessage message) {
		ByteBuffer byteBuffer = message.payload();
		if (byteBuffer != null) {
			System.out.println(ByteBufferUtil.toString(byteBuffer));
		}
	}

	@Override
	public void processUnSubAck(MqttUnsubAckMessage message) {
		System.out.println(message);
	}

	@Override
	public void processPubAck(MqttPubAckMessage message) {
		System.out.println(message);
	}

	@Override
	public void processPubRec(ChannelContext context, MqttMessage message) {
		System.out.println(message);
	}

	@Override
	public void processPubRel(ChannelContext context, MqttMessage message) {
		System.out.println(message);
	}

	@Override
	public void processPubComp(MqttMessage message) {
		System.out.println(message);
	}

}
