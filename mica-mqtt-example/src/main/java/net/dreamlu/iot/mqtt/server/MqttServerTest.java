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

package net.dreamlu.iot.mqtt.server;

import net.dreamlu.iot.mqtt.codec.*;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.server.ServerTioConfig;
import org.tio.utils.lock.SetWithLock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * mqtt 服务端测试
 *
 * @author L.cm
 */
public class MqttServerTest {

	public static void main(String[] args) throws IOException {
		MqttServer mqttServer = MqttServer.create()
			// 默认 MICA-MQTT-SERVER
			.name("mqtt-server")
			// 默认：127.0.0.1
			.ip("127.0.0.1")
			// 默认：1883
			.port(1883)
			.processor(new MqttBrokerProcessorImpl())
			.start();

		ServerTioConfig serverConfig = mqttServer.getServerConfig();

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				SetWithLock<ChannelContext> contextSet = Tio.getAll(serverConfig);
				Set<ChannelContext> channelContexts = contextSet.getObj();
				channelContexts.forEach(context -> {
					System.out.println(String.format("MqttServer send to clientId:%s", context.getBsId()));
					MqttPublishMessage message = (MqttPublishMessage) MqttMessageFactory.newMessage(
						new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
						new MqttPublishVariableHeader("/test/123", 0), ByteBuffer.wrap("mica最牛皮".getBytes()));
					Tio.send(context, message);
				});
			}
		}, 1000, 2000);
	}
}
