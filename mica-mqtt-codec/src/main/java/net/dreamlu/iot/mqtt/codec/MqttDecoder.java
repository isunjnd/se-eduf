/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package net.dreamlu.iot.mqtt.codec;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes Mqtt messages from bytes, following
 * <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html">
 * the MQTT protocol specification v3.1</a>
 */
public final class MqttDecoder {
	public static final MqttDecoder INSTANCE = new MqttDecoder();

	private static final int DEFAULT_MAX_BYTES_IN_MESSAGE = 8092;
	private static final int MQTT_PROTOCOL_LENGTH = 2;

	private final int maxBytesInMessage;

	public MqttDecoder() {
		this(DEFAULT_MAX_BYTES_IN_MESSAGE);
	}

	public MqttDecoder(int maxBytesInMessage) {
		this.maxBytesInMessage = maxBytesInMessage;
	}

	public boolean isSupport(ByteBuffer readBuffer) {
		return readBuffer.limit() >= MQTT_PROTOCOL_LENGTH;
	}

	public MqttMessage decode(ByteBuffer readBuffer) {
		// 1. 首先判断缓存中协议头是否读完（MQTT协议头为2字节）
		if (readBuffer.limit() < MQTT_PROTOCOL_LENGTH) {
			return null;
		}
		// 2. 解析 FixedHeader
		MqttFixedHeader mqttFixedHeader;
		int bytesRemainingInVariablePart;
		try {
			mqttFixedHeader = decodeFixedHeader(readBuffer);
			bytesRemainingInVariablePart = mqttFixedHeader.remainingLength();
		} catch (Exception cause) {
			return MqttMessageFactory.newInvalidMessage(cause);
		}
		// 3. 解析头信息
		Object variableHeader = null;
		try {
			Result<?> decodedVariableHeader = decodeVariableHeader(readBuffer, mqttFixedHeader);
			variableHeader = decodedVariableHeader.value;
			if (bytesRemainingInVariablePart > maxBytesInMessage) {
				throw new DecoderException("too large message: " + bytesRemainingInVariablePart + " bytes");
			}
			bytesRemainingInVariablePart -= decodedVariableHeader.numberOfBytesConsumed;
		} catch (Exception cause) {
			return MqttMessageFactory.newInvalidMessage(mqttFixedHeader, variableHeader, cause);
		}
		// 4. 解析消息体
		final Result<?> decodedPayload;
		try {
			decodedPayload = decodePayload(readBuffer,
				mqttFixedHeader.messageType(),
				bytesRemainingInVariablePart,
				variableHeader);
			bytesRemainingInVariablePart -= decodedPayload.numberOfBytesConsumed;
			if (bytesRemainingInVariablePart != 0) {
				throw new DecoderException("non-zero remaining payload bytes: " +
					bytesRemainingInVariablePart + " (" + mqttFixedHeader.messageType() + ')');
			}
			return MqttMessageFactory.newMessage(mqttFixedHeader, variableHeader, decodedPayload.value);
		} catch (Throwable cause) {
			return MqttMessageFactory.newInvalidMessage(mqttFixedHeader, variableHeader, cause);
		}
	}

	/**
	 * Decodes the fixed header. It's one byte for the flags and then variable bytes for the remaining length.
	 *
	 * @param buffer the buffer to decode from
	 * @return the fixed header
	 */
	private static MqttFixedHeader decodeFixedHeader(ByteBuffer buffer) {
		short b1 = ByteBufferUtil.readUnsignedByte(buffer);
		MqttMessageType messageType = MqttMessageType.valueOf(b1 >> 4);
		boolean dupFlag = (b1 & 0x08) == 0x08;
		int qosLevel = (b1 & 0x06) >> 1;
		boolean retain = (b1 & 0x01) != 0;

		int remainingLength = 0;
		int multiplier = 1;
		short digit;
		int loops = 0;
		do {
			digit = buffer.get();
			remainingLength += (digit & 127) * multiplier;
			multiplier *= 128;
			loops++;
		} while ((digit & 128) != 0 && loops < 4);
		// MQTT protocol limits Remaining Length to 4 bytes
		if (loops == 4 && (digit & 128) != 0) {
			throw new DecoderException("remaining length exceeds 4 digits (" + messageType + ')');
		}
		MqttFixedHeader decodedFixedHeader = new MqttFixedHeader(messageType, dupFlag, MqttQoS.valueOf(qosLevel), retain, remainingLength);
		return MqttCodecUtil.validateFixedHeader(MqttCodecUtil.resetUnusedFields(decodedFixedHeader));
	}

	/**
	 * Decodes the variable header (if any)
	 *
	 * @param buffer          the buffer to decode from
	 * @param mqttFixedHeader MqttFixedHeader of the same message
	 * @return the variable header
	 */
	private static Result<?> decodeVariableHeader(ByteBuffer buffer, MqttFixedHeader mqttFixedHeader) {
		switch (mqttFixedHeader.messageType()) {
			case CONNECT:
				return decodeConnectionVariableHeader(buffer);
			case CONNACK:
				return decodeConnAckVariableHeader(buffer);
			case SUBSCRIBE:
			case UNSUBSCRIBE:
			case SUBACK:
			case UNSUBACK:
			case PUBACK:
			case PUBREC:
			case PUBCOMP:
			case PUBREL:
				return decodeMessageIdVariableHeader(buffer);
			case PUBLISH:
				return decodePublishVariableHeader(buffer, mqttFixedHeader);
			case PINGREQ:
			case PINGRESP:
			case DISCONNECT:
				// Empty variable header
				return new Result<>(null, 0);
		}
		return new Result<>(null, 0);
	}

	private static Result<MqttConnectVariableHeader> decodeConnectionVariableHeader(ByteBuffer buffer) {
		final Result<String> protoString = decodeString(buffer);
		int numberOfBytesConsumed = protoString.numberOfBytesConsumed;

		final byte protocolLevel = buffer.get();
		numberOfBytesConsumed += 1;

		final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(protoString.value, protocolLevel);

		final int b1 = ByteBufferUtil.readUnsignedByte(buffer);
		numberOfBytesConsumed += 1;

		final Result<Integer> keepAlive = decodeMsbLsb(buffer);
		numberOfBytesConsumed += keepAlive.numberOfBytesConsumed;

		final boolean hasUserName = (b1 & 0x80) == 0x80;
		final boolean hasPassword = (b1 & 0x40) == 0x40;
		final boolean willRetain = (b1 & 0x20) == 0x20;
		final int willQos = (b1 & 0x18) >> 3;
		final boolean willFlag = (b1 & 0x04) == 0x04;
		final boolean cleanSession = (b1 & 0x02) == 0x02;
		if (mqttVersion == MqttVersion.MQTT_3_1_1) {
			final boolean zeroReservedFlag = (b1 & 0x01) == 0x0;
			if (!zeroReservedFlag) {
				// MQTT v3.1.1: The Server MUST validate that the reserved flag in the CONNECT Control Packet is
				// set to zero and disconnect the Client if it is not zero.
				// See http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc385349230
				throw new DecoderException("non-zero reserved flag");
			}
		}

		final MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(
			mqttVersion.protocolName(),
			mqttVersion.protocolLevel(),
			hasUserName,
			hasPassword,
			willRetain,
			willQos,
			willFlag,
			cleanSession,
			keepAlive.value);
		return new Result<>(mqttConnectVariableHeader, numberOfBytesConsumed);
	}

	private static Result<MqttConnAckVariableHeader> decodeConnAckVariableHeader(ByteBuffer buffer) {
		final boolean sessionPresent = (ByteBufferUtil.readUnsignedByte(buffer) & 0x01) == 0x01;
		byte returnCode = ByteBufferUtil.readByte(buffer);
		final int numberOfBytesConsumed = 2;
		final MqttConnAckVariableHeader mqttConnAckVariableHeader =
			new MqttConnAckVariableHeader(MqttConnectReturnCode.valueOf(returnCode), sessionPresent);
		return new Result<>(mqttConnAckVariableHeader, numberOfBytesConsumed);
	}

	private static Result<MqttMessageIdVariableHeader> decodeMessageIdVariableHeader(ByteBuffer buffer) {
		final Result<Integer> messageId = decodeMessageId(buffer);
		return new Result<>(MqttMessageIdVariableHeader.from(messageId.value),
			messageId.numberOfBytesConsumed);
	}

	private static Result<MqttPublishVariableHeader> decodePublishVariableHeader(
		ByteBuffer buffer,
		MqttFixedHeader mqttFixedHeader) {
		final Result<String> decodedTopic = decodeString(buffer);
		if (!MqttCodecUtil.isValidPublishTopicName(decodedTopic.value)) {
			throw new DecoderException("invalid publish topic name: " + decodedTopic.value + " (contains wildcards)");
		}
		int numberOfBytesConsumed = decodedTopic.numberOfBytesConsumed;

		int messageId = -1;
		if (mqttFixedHeader.qosLevel().value() > 0) {
			final Result<Integer> decodedMessageId = decodeMessageId(buffer);
			messageId = decodedMessageId.value;
			numberOfBytesConsumed += decodedMessageId.numberOfBytesConsumed;
		}
		final MqttPublishVariableHeader mqttPublishVariableHeader =
			new MqttPublishVariableHeader(decodedTopic.value, messageId);
		return new Result<>(mqttPublishVariableHeader, numberOfBytesConsumed);
	}

	private static Result<Integer> decodeMessageId(ByteBuffer buffer) {
		final Result<Integer> messageId = decodeMsbLsb(buffer);
		if (!MqttCodecUtil.isValidMessageId(messageId.value)) {
			throw new DecoderException("invalid messageId: " + messageId.value);
		}
		return messageId;
	}

	/**
	 * Decodes the payload.
	 *
	 * @param buffer                       the buffer to decode from
	 * @param messageType                  type of the message being decoded
	 * @param bytesRemainingInVariablePart bytes remaining
	 * @param variableHeader               variable header of the same message
	 * @return the payload
	 */
	private static Result<?> decodePayload(
		ByteBuffer buffer,
		MqttMessageType messageType,
		int bytesRemainingInVariablePart,
		Object variableHeader) {
		switch (messageType) {
			case CONNECT:
				return decodeConnectionPayload(buffer, (MqttConnectVariableHeader) variableHeader);
			case SUBSCRIBE:
				return decodeSubscribePayload(buffer, bytesRemainingInVariablePart);
			case SUBACK:
				return decodeSubackPayload(buffer, bytesRemainingInVariablePart);
			case UNSUBSCRIBE:
				return decodeUnsubscribePayload(buffer, bytesRemainingInVariablePart);
			case PUBLISH:
				return decodePublishPayload(buffer, bytesRemainingInVariablePart);
			default:
				// unknown payload , no byte consumed
				return new Result<>(null, 0);
		}
	}

	private static Result<MqttConnectPayload> decodeConnectionPayload(
		ByteBuffer buffer,
		MqttConnectVariableHeader mqttConnectVariableHeader) {
		final Result<String> decodedClientId = decodeString(buffer);
		final String decodedClientIdValue = decodedClientId.value;
		final MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(mqttConnectVariableHeader.name(),
			(byte) mqttConnectVariableHeader.version());
		if (!MqttCodecUtil.isValidClientId(mqttVersion, decodedClientIdValue)) {
			throw new MqttIdentifierRejectedException("invalid clientIdentifier: " + decodedClientIdValue);
		}
		int numberOfBytesConsumed = decodedClientId.numberOfBytesConsumed;

		Result<String> decodedWillTopic = null;
		Result<byte[]> decodedWillMessage = null;
		if (mqttConnectVariableHeader.isWillFlag()) {
			decodedWillTopic = decodeString(buffer, 0, 32767);
			numberOfBytesConsumed += decodedWillTopic.numberOfBytesConsumed;
			decodedWillMessage = decodeByteArray(buffer);
			numberOfBytesConsumed += decodedWillMessage.numberOfBytesConsumed;
		}
		Result<String> decodedUserName = null;
		Result<byte[]> decodedPassword = null;
		if (mqttConnectVariableHeader.hasUserName()) {
			decodedUserName = decodeString(buffer);
			numberOfBytesConsumed += decodedUserName.numberOfBytesConsumed;
		}
		if (mqttConnectVariableHeader.hasPassword()) {
			decodedPassword = decodeByteArray(buffer);
			numberOfBytesConsumed += decodedPassword.numberOfBytesConsumed;
		}

		final MqttConnectPayload mqttConnectPayload =
			new MqttConnectPayload(
				decodedClientId.value,
				decodedWillTopic != null ? decodedWillTopic.value : null,
				decodedWillMessage != null ? decodedWillMessage.value : null,
				decodedUserName != null ? decodedUserName.value : null,
				decodedPassword != null ? decodedPassword.value : null);
		return new Result<>(mqttConnectPayload, numberOfBytesConsumed);
	}

	private static Result<MqttSubscribePayload> decodeSubscribePayload(
		ByteBuffer buffer,
		int bytesRemainingInVariablePart) {
		final List<MqttTopicSubscription> subscribeTopics = new ArrayList<>();
		int numberOfBytesConsumed = 0;
		while (numberOfBytesConsumed < bytesRemainingInVariablePart) {
			final Result<String> decodedTopicName = decodeString(buffer);
			numberOfBytesConsumed += decodedTopicName.numberOfBytesConsumed;
			int qos = ByteBufferUtil.readUnsignedByte(buffer) & 0x03;
			numberOfBytesConsumed++;
			subscribeTopics.add(new MqttTopicSubscription(decodedTopicName.value, MqttQoS.valueOf(qos)));
		}
		return new Result<>(new MqttSubscribePayload(subscribeTopics), numberOfBytesConsumed);
	}

	private static Result<MqttSubAckPayload> decodeSubackPayload(
		ByteBuffer buffer,
		int bytesRemainingInVariablePart) {
		final List<Integer> grantedQos = new ArrayList<>();
		int numberOfBytesConsumed = 0;
		while (numberOfBytesConsumed < bytesRemainingInVariablePart) {
			int qos = ByteBufferUtil.readUnsignedByte(buffer);
			if (qos != MqttQoS.FAILURE.value()) {
				qos &= 0x03;
			}
			numberOfBytesConsumed++;
			grantedQos.add(qos);
		}
		return new Result<>(new MqttSubAckPayload(grantedQos), numberOfBytesConsumed);
	}

	private static Result<MqttUnsubscribePayload> decodeUnsubscribePayload(
		ByteBuffer buffer,
		int bytesRemainingInVariablePart) {
		final List<String> unsubscribeTopics = new ArrayList<>();
		int numberOfBytesConsumed = 0;
		while (numberOfBytesConsumed < bytesRemainingInVariablePart) {
			final Result<String> decodedTopicName = decodeString(buffer);
			numberOfBytesConsumed += decodedTopicName.numberOfBytesConsumed;
			unsubscribeTopics.add(decodedTopicName.value);
		}
		return new Result<>(new MqttUnsubscribePayload(unsubscribeTopics), numberOfBytesConsumed);
	}

	private static Result<ByteBuffer> decodePublishPayload(ByteBuffer buffer, int bytesRemainingInVariablePart) {
		byte[] slice = new byte[bytesRemainingInVariablePart];
		buffer.get(slice, 0, bytesRemainingInVariablePart);
		ByteBuffer byteBuffer = ByteBuffer.wrap(slice);
		return new Result<>(byteBuffer, bytesRemainingInVariablePart);
	}

	private static Result<String> decodeString(ByteBuffer buffer) {
		return decodeString(buffer, 0, Integer.MAX_VALUE);
	}

	private static Result<String> decodeString(ByteBuffer buffer, int minBytes, int maxBytes) {
		final Result<Integer> decodedSize = decodeMsbLsb(buffer);
		int size = decodedSize.value;
		int numberOfBytesConsumed = decodedSize.numberOfBytesConsumed;
		if (size < minBytes || size > maxBytes) {
			ByteBufferUtil.skipBytes(buffer, size);
			numberOfBytesConsumed += size;
			return new Result<>(null, numberOfBytesConsumed);
		}
		String s = new String(buffer.array(), buffer.position(), size, StandardCharsets.UTF_8);
		ByteBufferUtil.skipBytes(buffer, size);
		numberOfBytesConsumed += size;
		return new Result<>(s, numberOfBytesConsumed);
	}

	private static Result<byte[]> decodeByteArray(ByteBuffer buffer) {
		final Result<Integer> decodedSize = decodeMsbLsb(buffer);
		int size = decodedSize.value;
		byte[] bytes = new byte[size];
		buffer.get(bytes);
//		buffer.readBytes(bytes);
		return new Result<>(bytes, decodedSize.numberOfBytesConsumed + size);
	}

	private static Result<Integer> decodeMsbLsb(ByteBuffer buffer) {
		return decodeMsbLsb(buffer, 0, 65535);
	}

	private static Result<Integer> decodeMsbLsb(ByteBuffer buffer, int min, int max) {
		short msbSize = ByteBufferUtil.readUnsignedByte(buffer);
		short lsbSize = ByteBufferUtil.readUnsignedByte(buffer);
		final int numberOfBytesConsumed = 2;
		int result = msbSize << 8 | lsbSize;
		if (result < min || result > max) {
			result = -1;
		}
		return new Result<>(result, numberOfBytesConsumed);
	}

    private static final class Result<T> {
		private final T value;
		private final int numberOfBytesConsumed;

		Result(T value, int numberOfBytesConsumed) {
			this.value = value;
			this.numberOfBytesConsumed = numberOfBytesConsumed;
		}
	}

}
