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

package net.dreamlu.iot.mqtt.core.server;

/**
 * 常量
 *
 * @author L.cm
 */
public interface MqttConst {

	/**
	 * 正常断开连接
	 */
	String DIS_CONNECTED = "disconnected";
	/**
	 * 是 http 协议
	 */
	String IS_HTTP = "is_http";
	/**
	 * session 有效期，小于等于 0，关闭时清理，大于 0 采用缓存处理
	 */
	String SESSION_EXPIRES = "session_expires";

}
