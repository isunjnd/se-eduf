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

package net.dreamlu.iot.mqtt.core.server;

import net.dreamlu.iot.mqtt.codec.ByteBufferAllocator;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.event.IMqttConnectStatusListener;
import net.dreamlu.iot.mqtt.core.server.event.IMqttMessageListener;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.session.InMemoryMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import net.dreamlu.iot.mqtt.core.server.store.InMemoryMqttMessageStore;
import net.dreamlu.iot.mqtt.core.server.support.*;
import org.tio.core.ssl.SslConfig;
import org.tio.core.stat.IpStatListener;
import org.tio.server.ServerTioConfig;
import org.tio.server.TioServer;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;
import org.tio.utils.thread.pool.DefaultThreadFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * mqtt 服务端参数构造
 *
 * @author L.cm
 */
public class MqttServerCreator {

	/**
	 * 名称
	 */
	private String name = "Mica-Mqtt-Server";
	/**
	 * 服务端 ip
	 */
	private String ip = "127.0.0.1";
	/**
	 * 端口
	 */
	private int port = 1883;
	/**
	 * 心跳超时时间(单位: 毫秒 默认: 1000 * 120)，如果用户不希望框架层面做心跳相关工作，请把此值设为0或负数
	 */
	private Long heartbeatTimeout;
	/**
	 * 堆内存和堆外内存
	 */
	private ByteBufferAllocator bufferAllocator = ByteBufferAllocator.HEAP;
	/**
	 * 接收数据的buffer size
	 */
	private Integer readBufferSize;
	/**
	 * ssl 证书配置
	 */
	private SslConfig sslConfig;
	/**
	 * tio 的 IpStatListener
	 */
	private IpStatListener ipStatListener;
	/**
	 * 认证处理器
	 */
	private IMqttServerAuthHandler authHandler;
	/**
	 * 消息处理器
	 */
	private IMqttMessageDispatcher messageDispatcher;
	/**
	 * 消息存储
	 */
	private IMqttMessageStore messageStore;
	/**
	 * session 管理
	 */
	private IMqttSessionManager sessionManager;
	/**
	 * 订阅管理
	 */
	private IMqttServerSubscribeManager subscribeManager;
	/**
	 * 消息监听
	 */
	private IMqttMessageListener messageListener;
	/**
	 * 链接状态监听
	 */
	private IMqttConnectStatusListener connectStatusListener;
	/**
	 * debug
	 */
	private boolean debug = false;

	public String getName() {
		return name;
	}

	public MqttServerCreator name(String name) {
		this.name = name;
		return this;
	}

	public String getIp() {
		return ip;
	}

	public MqttServerCreator ip(String ip) {
		this.ip = ip;
		return this;
	}

	public int getPort() {
		return port;
	}

	public MqttServerCreator port(int port) {
		this.port = port;
		return this;
	}

	public Long getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public MqttServerCreator heartbeatTimeout(Long heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
		return this;
	}

	public ByteBufferAllocator getBufferAllocator() {
		return bufferAllocator;
	}

	public MqttServerCreator bufferAllocator(ByteBufferAllocator bufferAllocator) {
		this.bufferAllocator = bufferAllocator;
		return this;
	}

	public Integer getReadBufferSize() {
		return readBufferSize;
	}

	public MqttServerCreator readBufferSize(Integer readBufferSize) {
		this.readBufferSize = readBufferSize;
		return this;
	}

	public SslConfig getSslConfig() {
		return sslConfig;
	}

	public MqttServerCreator useSsl(InputStream keyStoreInputStream, InputStream trustStoreInputStream, String pwd) {
		try {
			this.sslConfig = SslConfig.forServer(keyStoreInputStream, trustStoreInputStream, pwd);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return this;
	}

	public MqttServerCreator useSsl(String keyStoreFile, String trustStoreFile, String pwd) {
		try {
			this.sslConfig = SslConfig.forServer(keyStoreFile, trustStoreFile, pwd);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return this;
	}

	public IpStatListener getIpStatListener() {
		return ipStatListener;
	}

	public MqttServerCreator ipStatListener(IpStatListener ipStatListener) {
		this.ipStatListener = ipStatListener;
		return this;
	}

	public IMqttServerAuthHandler getAuthHandler() {
		return authHandler;
	}

	public MqttServerCreator authHandler(IMqttServerAuthHandler authHandler) {
		this.authHandler = authHandler;
		return this;
	}

	public IMqttMessageStore getMessageStore() {
		return messageStore;
	}

	public MqttServerCreator messageStore(IMqttMessageStore messageStore) {
		this.messageStore = messageStore;
		return this;
	}

	public IMqttSessionManager getSessionManager() {
		return sessionManager;
	}

	public MqttServerCreator sessionManager(IMqttSessionManager sessionManager) {
		this.sessionManager = sessionManager;
		return this;
	}

	public IMqttServerSubscribeManager getSubscribeManager() {
		return subscribeManager;
	}

	public MqttServerCreator subscribeManager(IMqttServerSubscribeManager subscribeManager) {
		this.subscribeManager = subscribeManager;
		return this;
	}

	public IMqttMessageListener getMessageListener() {
		return messageListener;
	}

	public MqttServerCreator messageListener(IMqttMessageListener messageListener) {
		this.messageListener = messageListener;
		return this;
	}

	public IMqttConnectStatusListener getConnectStatusListener() {
		return connectStatusListener;
	}

	public MqttServerCreator connectStatusListener(IMqttConnectStatusListener connectStatusListener) {
		this.connectStatusListener = connectStatusListener;
		return this;
	}

	public boolean isDebug() {
		return debug;
	}

	public MqttServerCreator debug() {
		this.debug = true;
		return this;
	}

	public MqttServer start() throws IOException {
		Objects.requireNonNull(this.messageListener, "Mqtt Server message listener cannot be null.");
		if (this.authHandler == null) {
			this.authHandler = new DefaultMqttServerAuthHandler();
		}
		if (this.messageDispatcher == null) {
			this.messageDispatcher = new DefaultMqttMessageDispatcher();
		}
		if (this.sessionManager == null) {
			this.sessionManager = new InMemoryMqttSessionManager();
		}
		if (this.subscribeManager == null) {
			this.subscribeManager = new DefaultMqttServerSubscribeManager();
		}
		if (this.messageStore == null) {
			this.messageStore = new InMemoryMqttMessageStore();
		}
		if (this.connectStatusListener == null) {
			this.connectStatusListener = new DefaultMqttConnectStatusListener();
		}
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2, DefaultThreadFactory.getInstance("MqttServer"));
		DefaultMqttServerProcessor serverProcessor = new DefaultMqttServerProcessor(
			this.messageStore, this.sessionManager, this.authHandler, this.subscribeManager,
			this.messageDispatcher, this.connectStatusListener, this.messageListener, executor);
		// 处理消息
		ServerAioHandler handler = new MqttServerAioHandler(this.bufferAllocator, serverProcessor);
		// 监听
		ServerAioListener listener = new MqttServerAioListener(
			this.messageStore, this.sessionManager, this.messageDispatcher, this.connectStatusListener);
		// 配置
		ServerTioConfig config = new ServerTioConfig(this.name, handler, listener);
		// 设置心跳 timeout
		if (this.heartbeatTimeout != null) {
			config.setHeartbeatTimeout(this.heartbeatTimeout);
		}
		if (this.ipStatListener != null) {
			config.setIpStatListener(this.ipStatListener);
		}
		if (this.readBufferSize != null) {
			config.setReadBufferSize(readBufferSize);
		}
		if (this.sslConfig != null) {
			config.setSslConfig(this.sslConfig);
		}
		if (this.debug) {
			config.debug = true;
		}
		TioServer tioServer = new TioServer(config);
		// 不校验版本号，社区版设置无效
		tioServer.setCheckLastVersion(false);
		// 启动
		tioServer.start(this.ip, this.port);
		MqttServer mqttServer = new MqttServer(tioServer, this.sessionManager, this.subscribeManager, executor);
		messageDispatcher.config(mqttServer);
		return mqttServer;
	}

}
