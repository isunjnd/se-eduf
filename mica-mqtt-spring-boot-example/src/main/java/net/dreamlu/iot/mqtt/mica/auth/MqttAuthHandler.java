package net.dreamlu.iot.mqtt.mica.auth;

import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.codec.MqttTopicSubscription;
import net.dreamlu.iot.mqtt.core.server.IMqttServerAuthHandler;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * mqtt tcp websocket 认证
 *
 * @author L.cm
 */
@Configuration(proxyBeanMethods = false)
public class MqttAuthHandler implements IMqttServerAuthHandler {

	@Override
	public boolean authenticate(String clientId, String userName, String password) {
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
