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
import net.dreamlu.iot.mqtt.core.server.IMqttServerSubscribeManager;
import net.dreamlu.iot.mqtt.core.server.model.Subscribe;
import net.dreamlu.iot.mqtt.core.util.MqttTopicUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 默认的 mqtt 订阅管理
 *
 * @author L.cm
 */
public class DefaultMqttServerSubscribeManager implements IMqttServerSubscribeManager {
	/**
	 * topicFilter: {clientId: SubscribeStore}
	 */
	private final ConcurrentMap<String, ConcurrentMap<String, Subscribe>> subscribeStore = new ConcurrentHashMap<>();

	@Override
	public void add(String topicFilter, String clientId, MqttQoS mqttQoS) {
		Map<String, Subscribe> data = subscribeStore.computeIfAbsent(topicFilter, (key) -> new ConcurrentHashMap<>(16));
		data.put(clientId, new Subscribe(topicFilter, mqttQoS.value()));
	}

	@Override
	public void remove(String topicFilter, String clientId) {
		ConcurrentMap<String, Subscribe> map = subscribeStore.get(topicFilter);
		if (map == null) {
			return;
		}
		map.remove(clientId);
	}

	@Override
	public void remove(String clientId) {
		subscribeStore.forEach((key, value) -> value.remove(clientId));
	}

	@Override
	public List<Subscribe> search(String topicName, String clientId) {
		List<Subscribe> list = new ArrayList<>();
		Set<String> topicFilterSet = subscribeStore.keySet();
		for (String topicFilter : topicFilterSet) {
			if (MqttTopicUtil.getTopicPattern(topicFilter).matcher(topicName).matches()) {
				ConcurrentMap<String, Subscribe> data = subscribeStore.get(topicFilter);
				if (data != null && !data.isEmpty()) {
					Subscribe subscribe = data.get(clientId);
					if (subscribe != null) {
						list.add(subscribe);
					}
				}
			}
		}
		return list;
	}

	@Override
	public void clean() {
		subscribeStore.clear();
	}

}
