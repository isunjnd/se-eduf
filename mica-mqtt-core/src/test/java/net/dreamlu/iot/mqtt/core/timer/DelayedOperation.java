/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a>
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.iot.mqtt.core.timer;

import net.dreamlu.iot.mqtt.core.util.timer.TimerTask;

import java.util.Date;

/**
 * DelayedOperation
 *
 * @author kafka、guest、L.cm
 */
public class DelayedOperation extends TimerTask {

	public DelayedOperation(long delayMs) {
		super(delayMs);
	}

	@Override
	public void run() {
		System.out.println(" do the job" + new Date());
	}

}
