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
package org.springframework.cloud.cloudfoundry.oauth2;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Dave Syer
 *
 */
@ConfigurationProperties("oauth2.resource")
@Data
public class ResourceServerProperties {

	private String serviceId = "resource";

	private String id;

	@Value("${vcap.services.${oauth2.resource.serviceId:resource}.credentials.clientId:}")
	private String clientId;

	@Value("${vcap.services.${oauth2.resource.serviceId:resource}.credentials.clientSecret:}")
	private String clientSecret;

	@Value("${vcap.services.${oauth2.resource.serviceId:sso}.credentials.userInfoUri:}")
	private String userInfoUri;

	@Value("${vcap.services.${oauth2.resource.serviceId:sso}.credentials.tokenInfoUri:}")
	private String tokenInfoUri;

	private boolean preferTokenInfo = true;

	public String getResourceId() {
		return id==null ? clientId : id;
	}
}
