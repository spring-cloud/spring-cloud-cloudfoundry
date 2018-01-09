/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.cloudfoundry.discovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Josh Long
 */
@ConfigurationProperties(prefix = "spring.cloud.cloudfoundry.discovery")
public class CloudFoundryDiscoveryProperties {

	/**
	 * Flag to indicate that discovery is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Frequency in milliseconds of poll for heart beat. The client will poll on this
	 * frequency and broadcast a list of service ids.
	 */
	private long heartbeatFrequency = 5000;

	/**
	 * Port to use when no port is defined by ribbon.
	 */
	private int defaultServerPort = 80;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public long getHeartbeatFrequency() {
		return this.heartbeatFrequency;
	}

	public void setHeartbeatFrequency(long heartbeatFrequency) {
		this.heartbeatFrequency = heartbeatFrequency;
	}

	public int getDefaultServerPort() {
		return defaultServerPort;
	}

	public void setDefaultServerPort(int defaultServerPort) {
		this.defaultServerPort = defaultServerPort;
	}

	@Override
	public String toString() {
		return "CloudFoundryDiscoveryProperties{" +
				"enabled=" + enabled +
				", heartbeatFrequency=" + heartbeatFrequency +
				", defaultServerPort=" + defaultServerPort +
				'}';
	}
}
