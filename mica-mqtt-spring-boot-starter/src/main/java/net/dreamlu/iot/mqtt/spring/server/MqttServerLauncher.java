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

package net.dreamlu.iot.mqtt.spring.server;

import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.MqttServerCreator;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;

/**
 * MqttServer 启动器
 *
 * @author L.cm
 */
public class MqttServerLauncher implements SmartLifecycle, Ordered {
	private final MqttServerCreator serverCreator;
	private MqttServer mqttServer;
	private Thread mqttServerThread;
	private boolean running = false;

	public MqttServerLauncher(MqttServerCreator serverCreator) {
		this.serverCreator = serverCreator;
	}

	@Override
	public void start() {
		mqttServerThread = new Thread(() -> {
			mqttServer = serverCreator.start();
			running = true;
		});
		mqttServerThread.setDaemon(true);
		mqttServerThread.start();
	}

	@Override
	public void stop() {
		if (mqttServer != null) {
			mqttServer.stop();
		}
		if (mqttServerThread != null) {
			mqttServerThread.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public int getPhase() {
		return DEFAULT_PHASE;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
