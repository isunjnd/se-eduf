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

package net.dreamlu.iot.mqtt.broker.auth;

import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.codec.MqttTopicSubscription;
import net.dreamlu.iot.mqtt.core.server.IMqttServerAuthHandler;
import org.springframework.context.annotation.Configuration;
import org.tio.core.ChannelContext;
import org.tio.core.Node;

import java.util.List;

/**
 * mqtt tcp websocket 认证
 *
 * @author L.cm
 */
@Configuration(proxyBeanMethods = false)
public class MqttAuthHandler implements IMqttServerAuthHandler {

	@Override
	public boolean authenticate(ChannelContext context, String clientId, String userName, String password) {
		// 获取客户端信息
		Node clientNode = context.getClientNode();
		// 客户端认证逻辑实现
		return true;
	}

	@Override
	public boolean isValidSubscribe(List<MqttTopicSubscription> topicSubscriptionList) {
		// 校验客户端订阅的 topic，校验成功返回 true，失败返回 false
		for (MqttTopicSubscription subscription : topicSubscriptionList) {
			String topicName = subscription.topicName();
			MqttQoS mqttQoS = subscription.qualityOfService();
		}
		return true;
	}
}
