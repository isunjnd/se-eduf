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
 * mqtt 服务端，认证处理器
 *
 * @author L.cm
 */
@FunctionalInterface
public interface IMqttServerAuthHandler {

	/**
	 * 认证
	 *
	 * @param clientId 客户端 ID
	 * @param userName 用户名
	 * @param password 密码
	 * @return 是否认证成功
	 */
	boolean authenticate(String clientId, String userName, String password);

}
