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

package net.dreamlu.iot.mqtt.broker.listener;

import net.dreamlu.iot.mqtt.codec.MqttMessageType;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.event.IMqttMessageListener;
import net.dreamlu.iot.mqtt.core.server.model.Message;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * 集群消息监听器
 *
 * @author L.cm
 */
public class MqttBrokerMessageListener implements IMqttMessageListener {
	private final IMqttMessageDispatcher dispatcher;

	public MqttBrokerMessageListener(IMqttMessageDispatcher dispatcher) {
		this.dispatcher = Objects.requireNonNull(dispatcher, "MqttMessageDispatcher is null.");
	}

	@Override
	public void onMessage(String clientId, String topic, MqttQoS mqttQoS, ByteBuffer payload) {
		Message message = new Message();
		message.setTopic(topic);
		message.setQos(mqttQoS.value());
		if (payload != null) {
			message.setPayload(payload.array());
		}
		message.setMessageType(MqttMessageType.PUBLISH.value());
		message.setStoreTime(System.currentTimeMillis());
		dispatcher.send(message);
	}
}
