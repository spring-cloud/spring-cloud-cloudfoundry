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

package org.springframework.cloud.cloudfoundry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration properties for a connection to a Cloud Foundry platform.
 *
 * @author Josh Long
 * @author Scott Frederick
 */
@ConfigurationProperties(prefix = "spring.cloud.cloudfoundry")
public class CloudFoundryProperties implements InitializingBean {

	/**
	 * URL of Cloud Foundry API (Cloud Controller).
	 */
	@Value("${vcap.application.cf_api:api.run.pivotal.io}")
	private String url;

	/**
	 * Username to authenticate (usually an email address).
	 */
	private String username;

	/**
	 * Password for user to authenticate and obtain token.
	 */
	private String password;

	/**
	 * Organization name to initially target.
	 */
	private String org;

	/**
	 * Space name to initially target.
	 */
	@Value("${vcap.application.space_name:}")
	private String space;

	private boolean skipSslValidation;

	public String getUrl() {
		return this.url;
	}

	private String safeUrl(String t) {
		String input = t.trim().toLowerCase();
		Pattern p = Pattern.compile("(http(s)?://)(.*)");
		Matcher matcher = p.matcher(input);
		if (matcher.matches()) {
			String group = matcher.group(1);
			if (StringUtils.hasText(group)) {
				return t.substring(group.length());
			}
		}
		return t;
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

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public boolean getSkipSslValidation() {
		return this.skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	@Override
	public void afterPropertiesSet() {
		this.url = safeUrl(this.url);
		this.password = this.password.trim();
		this.username = this.username.trim();
		if (this.org != null) {
			this.org = this.org.trim();
		}
		if (this.space != null) {
			this.space = this.space.trim();
		}

		Map<String, String> vals = new HashMap<>();
		vals.put("url", getUrl());
		vals.put("username", getUsername());
		vals.put("password", getPassword());
		vals.forEach((key, value) -> Assert.hasText(value, String.format("'%s' must be provided", key)));
	}
}