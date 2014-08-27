/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.platform.cloudfoundry.sso;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author Dave Syer
 *
 */
@ConfigurationProperties("cloudfoundry.sso")
@Data
public class CloudfoundrySsoProperties implements Validator {

	@Value("${vcap.services.sso.credentials.tokenUri:}")
	private String tokenUri;

	@Value("${vcap.services.sso.credentials.authorizationUri:}")
	private String authorizationUri;

	@Value("${vcap.services.sso.credentials.clientId:}")
	private String clientId;

	@Value("${vcap.services.sso.credentials.clientSecret:}")
	private String clientSecret;
	
	private Home home = new Home();

	@Data
	public static class Home {		
		private String path = "/";		
		private boolean secure = true;
	}

	public String getTokenInfoUri() {
		return tokenUri.replace("/oauth/token", "/check_token");
	}

	public String getUserInfoUri() {
		return tokenUri.replace("/oauth/token", "/user_info");
	}

	public String getLogoutUri(String redirectUrl) {
		return tokenUri.replace("/oauth/token", "/logout.do?redirect="+redirectUrl);
	}
	
	@Override
	public boolean supports(Class<?> clazz) {
		return CloudfoundrySsoProperties.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		CloudfoundrySsoProperties sso = (CloudfoundrySsoProperties) target;
		if (StringUtils.hasText(sso.getTokenUri())) {
			if (!StringUtils.hasText(sso.getAuthorizationUri())) {
				errors.rejectValue("authorizeUri", "missing.authorizeUri", "Missing authorizeUri");
			}
			if (!StringUtils.hasText(sso.getClientId())) {
				errors.rejectValue("clientId", "missing.clientId", "Missing clientId");
			}
			if (!StringUtils.hasText(sso.getClientSecret())) {
				errors.rejectValue("clientSecret", "missing.clientSecret", "Missing clientSecret");
			}
		}
	}

}
