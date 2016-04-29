/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Josh Long
 */
@ConfigurationProperties(prefix = "spring.cloud.cloudfoundry.discovery")
public class CloudFoundryDiscoveryProperties {

	/**
	 * URL of Cloud Foundry API (Cloud Controller).
	 */
	private String url = "https://api.run.pivotal.io";

	/**
	 * Username to authenticate (usually an email address).
	 */
	private String username;

	/**
	 * Password for user to authenticate and obtain token.
	 */
	private String password;

	/**
	 * Organization name to authenticate with (default to user's default).
	 */
	private String org;

	/**
	 * Space name to authenticate with (default to user's default).
	 */
	@Value("${vcap.application.space_name:}")
	private String space;

	/**
	 * Flag to indicate that discovery is enabled.
	 */
	private boolean enabled = true;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String cloudControllerUrl) {
		this.url = cloudControllerUrl;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String email) {
		this.username = email;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOrg() {
		return this.org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getSpace() {
		return this.space;
	}

	public void setSpace(String space) {
		this.space = space;
	}
}