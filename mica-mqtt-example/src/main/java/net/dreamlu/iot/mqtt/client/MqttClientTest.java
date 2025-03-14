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

package net.dreamlu.iot.mqtt.client;

import net.dreamlu.iot.mqtt.codec.ByteBufferUtil;
import net.dreamlu.iot.mqtt.codec.MqttVersion;
import net.dreamlu.iot.mqtt.core.client.MqttClient;
import org.tio.utils.thread.pool.DefaultThreadFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 客户端测试
 *
 * @author L.cm
 */
public class MqttClientTest {

	public static void main(String[] args) throws Exception {
		// 初始化 mqtt 客户端
		MqttClient client = MqttClient.create()
			.ip("127.0.0.1")
			.username("admin")
			.password("123456")
			.protocolVersion(MqttVersion.MQTT_5)
			.connect();

		client.subQos0("/test/#", (topic, payload) -> {
			System.out.println(ByteBufferUtil.toString(payload));
		});

		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, DefaultThreadFactory.getInstance("MqttClient"));

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				System.out.println("TimerTask run");
			}
		};
		executor.schedule(() -> {}, 1, TimeUnit.SECONDS);

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				client.publish("testtopicxx", ByteBuffer.wrap("mica最牛皮".getBytes(StandardCharsets.UTF_8)));
			}
		}, 1000, 2000);
	}
}
