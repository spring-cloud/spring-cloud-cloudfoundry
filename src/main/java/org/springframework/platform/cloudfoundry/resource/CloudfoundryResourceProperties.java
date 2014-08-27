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
package org.springframework.platform.cloudfoundry.resource;

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
@ConfigurationProperties("cloudfoundry.resource")
@Data
public class CloudfoundryResourceProperties implements Validator {

	private String serviceId = "resource";

	@Value("${vcap.services.${cloudfoundry.resource.serviceId:resource}.credentials.clientId:}")
	private String clientId;

	@Value("${vcap.services.${cloudfoundry.resource.serviceId:resource}.credentials.clientSecret:}")
	private String clientSecret;

	@Value("${vcap.services.${cloudfoundry.resource.serviceId:resource}.credentials.tokenInfoUri:}")
	private String tokenInfoUri;


	@Override
	public boolean supports(Class<?> clazz) {
		return CloudfoundryResourceProperties.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		CloudfoundryResourceProperties resource = (CloudfoundryResourceProperties) target;
		if (StringUtils.hasText(resource.getClientId())) {
			if (!StringUtils.hasText(resource.getTokenInfoUri())) {
				errors.rejectValue("tokenInfoUri", "missing.tokenInfoUri", "Missing tokenInfoUri");
			}
			if (!StringUtils.hasText(resource.getClientSecret())) {
				errors.rejectValue("clientSecret", "missing.clientSecret", "Missing clientSecret");
			}
		}
	}
}
