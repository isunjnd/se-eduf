package net.dreamlu.iot.mqtt.core.client;

import net.dreamlu.iot.mqtt.codec.MqttMessage;
import net.dreamlu.iot.mqtt.codec.MqttPublishMessage;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * MqttPendingPublish，参考于 netty-mqtt-client
 */
final class MqttPendingQos2Publish {
	private final MqttPublishMessage incomingPublish;
	private final RetryProcessor<MqttMessage> retryProcessor = new RetryProcessor<>();

	MqttPendingQos2Publish(MqttPublishMessage incomingPublish, MqttMessage originalMessage) {
		this.incomingPublish = incomingPublish;
		this.retryProcessor.setOriginalMessage(originalMessage);
	}

	MqttPublishMessage getIncomingPublish() {
		return incomingPublish;
	}

	void startPubRecRetransmitTimer(ScheduledThreadPoolExecutor executor, Consumer<MqttMessage> sendPacket) {
		this.retryProcessor.setHandle((fixedHeader, originalMessage) ->
			sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
		this.retryProcessor.start(executor);
	}

	void onPubRelReceived() {
		this.retryProcessor.stop();
	}

}
