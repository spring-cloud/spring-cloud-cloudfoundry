package org.springframework.cloud.cloudfoundry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@ConfigurationProperties(prefix = "spring.cloud.cloudfoundry")
public class CloudFoundryProperties implements InitializingBean {

	/**
	 * URL of Cloud Foundry API (Cloud Controller).
	 */
	private String url = "api.run.pivotal.io";

	/**
	 * Username to authenticate (usually an email address).
	 */
	@NonNull
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
	public void afterPropertiesSet() throws Exception {
		this.url = safeUrl(this.url);
		this.password = this.password.trim();
		this.username = this.username.trim();
		this.org = this.org.trim();
		this.space = this.space.trim();

		Map<String, String> vals = new HashMap<>();
		vals.put("org", getOrg());
		vals.put("url", getUrl());
		vals.put("username", getUsername());
		vals.put("password", getPassword());
		vals.put("space", getSpace());
		vals.forEach((key, value) -> Assert.hasText(value, String.format("'%s' must be provided", key)));
	}
}