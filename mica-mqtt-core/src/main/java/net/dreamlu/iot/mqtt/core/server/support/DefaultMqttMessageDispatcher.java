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

package net.dreamlu.iot.mqtt.core.server.support;

import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.model.Message;

import java.nio.ByteBuffer;

/**
 * 默认的消息转发器
 *
 * @author L.cm
 */
public class DefaultMqttMessageDispatcher implements IMqttMessageDispatcher {
	private MqttServer mqttServer;

	@Override
	public void config(MqttServer mqttServer) {
		this.mqttServer = mqttServer;
	}

	@Override
	public boolean send(Message message) {
		if (mqttServer == null) {
			return false;
		}
		ByteBuffer payload = ByteBuffer.wrap(message.getPayload());
		MqttQoS qoS = MqttQoS.valueOf(message.getQos());
		return mqttServer.publishAll(message.getTopic(), payload, qoS);
	}

	@Override
	public boolean send(String clientId, Message message) {
		if (mqttServer == null) {
			return false;
		}
		ByteBuffer payload = ByteBuffer.wrap(message.getPayload());
		MqttQoS qoS = MqttQoS.valueOf(message.getQos());
		return mqttServer.publish(clientId, message.getTopic(), payload, qoS);
	}
}
