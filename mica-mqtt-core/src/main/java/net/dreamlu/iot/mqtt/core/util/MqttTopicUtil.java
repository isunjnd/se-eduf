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

package net.dreamlu.iot.mqtt.core.util;

import net.dreamlu.iot.mqtt.codec.MqttCodecUtil;

import java.util.regex.Pattern;

/**
 * Mqtt Topic 工具
 *
 * @author L.cm
 */
public final class MqttTopicUtil {

	/**
	 * 判断 topicFilter topicName 是否匹配
	 *
	 * @param topicFilter topicFilter
	 * @param topicName   topicName
	 * @return 是否匹配
	 */
	public static boolean match(String topicFilter, String topicName) {
		if (MqttCodecUtil.isTopicFilter(topicFilter)) {
			return getTopicPattern(topicFilter).matcher(topicName).matches();
		} else {
			return topicFilter.equals(topicName);
		}
	}

	/**
	 * mqtt topicFilter 转正则
	 *
	 * @param topicFilter topicFilter
	 * @return Pattern
	 */
	public static Pattern getTopicPattern(String topicFilter) {
		// mqtt 分享主题 $share/{ShareName}/{filter}
		String topicRegex = topicFilter.startsWith("$") ? "\\" + topicFilter : topicFilter;
		return Pattern.compile(topicRegex
			.replace("+", "[^/]+")
			.replace("#", ".+")
			.concat("$")
		);
	}

}
