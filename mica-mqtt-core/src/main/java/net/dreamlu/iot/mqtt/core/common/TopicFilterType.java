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

package net.dreamlu.iot.mqtt.core.common;

import net.dreamlu.iot.mqtt.core.util.TopicUtil;

/**
 * TopicFilter 类型
 *
 * @author L.cm
 */
public enum TopicFilterType {

	/**
	 * 默认 TopicFilter
	 */
	NONE {
		@Override
		public boolean match(String topicFilter, String topicName) {
			return TopicUtil.match(topicFilter, topicName);
		}
	},

	/**
	 * $queue/ 为前缀的共享订阅是不带群组的共享订阅
	 */
	QUEUE {
		@Override
		public boolean match(String topicFilter, String topicName) {
			int prefixLen = TopicFilterType.SHARE_QUEUE_PREFIX.length();
			if (topicName.startsWith("/")) {
				prefixLen = prefixLen - 1;
			}
			return TopicUtil.match(topicFilter.substring(prefixLen), topicName);
		}
	},

	/**
	 * $share/<group-name>/ 为前缀的共享订阅是带群组的共享订阅
	 */
	SHARE {
		@Override
		public boolean match(String topicFilter, String topicName) {
			String shareTopicFilter = topicFilter.substring(TopicFilterType.SHARE_GROUP_PREFIX.length());
			String[] group = shareTopicFilter.split("/");
			String groupName = group[0];
			String shareTopicPrefix = TopicFilterType.SHARE_GROUP_PREFIX + groupName + '/';
			int prefixLen = shareTopicPrefix.length();
			if (topicName.startsWith("/")) {
				prefixLen = prefixLen - 1;
			}
			return TopicUtil.match(topicFilter.substring(prefixLen), topicName);
		}
	};

	/**
	 * 共享订阅的 topic
	 */
	public static final String SHARE_QUEUE_PREFIX = "$queue/";
	public static final String SHARE_GROUP_PREFIX = "$share/";

	/**
	 * 判断 topicFilter 和 topicName 匹配情况
	 *
	 * @param topicFilter topicFilter
	 * @param topicName   topicName
	 * @return 是否匹配
	 */
	public abstract boolean match(String topicFilter, String topicName);

	/**
	 * 获取 topicFilter 类型
	 *
	 * @param topicFilter topicFilter
	 * @return TopicFilterType
	 */
	public static TopicFilterType getType(String topicFilter) {
		if (topicFilter.startsWith(TopicFilterType.SHARE_QUEUE_PREFIX)) {
			return TopicFilterType.QUEUE;
		} else if (topicFilter.startsWith(TopicFilterType.SHARE_GROUP_PREFIX)) {
			return TopicFilterType.SHARE;
		} else {
			return TopicFilterType.NONE;
		}
	}

}
