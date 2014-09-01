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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties(ResourceServerProperties.class)
public class ResourceServerTokenServicesConfiguration {

	@Autowired
	private ResourceServerProperties resource;

	@Bean
	@ConditionalOnMissingBean(ResourceServerTokenServices.class)
	@ConditionalOnExpression("${oauth2.resource.preferTokenInfo:${OAUTH2_RESOURCE_PREFERTOKENINFO:true}}")
	protected RemoteTokenServices remoteTokenServices() {
		RemoteTokenServices services = new RemoteTokenServices();
		services.setCheckTokenEndpointUrl(resource.getTokenInfoUri());
		services.setClientId(resource.getClientId());
		services.setClientSecret(resource.getClientSecret());
		return services;
	}

	@Configuration
	@ConditionalOnClass(OAuth2ConnectionFactory.class)
	@ConditionalOnExpression("!${oauth2.resource.preferTokenInfo:${OAUTH2_RESOURCE_PREFERTOKENINFO:true}}")
	protected static class SocialTokenServicesConfiguration {

		@Autowired
		private ResourceServerProperties sso;

		@Autowired(required = false)
		private OAuth2ConnectionFactory<?> connectionFactory;

		@Autowired(required = false)
		@Qualifier("oauth2RestTemplate")
		private OAuth2RestOperations restTemplate;

		@Bean
		@ConditionalOnBean(OAuth2ConnectionFactory.class)
		@ConditionalOnMissingBean(ResourceServerTokenServices.class)
		public SpringSocialTokenServices socialTokenServices() {
			return new SpringSocialTokenServices(connectionFactory, sso.getClientId());
		}

		@Bean
		@ConditionalOnMissingBean({ OAuth2ConnectionFactory.class,
				ResourceServerTokenServices.class })
		public UserInfoTokenServices userInfoTokenServices() {
			return new UserInfoTokenServices(restTemplate, sso.getUserInfoUri(),
					sso.getClientId());
		}

	}

	@Configuration
	@ConditionalOnMissingClass(name = "org.springframework.social.connect.support.OAuth2ConnectionFactory")
	@ConditionalOnExpression("!${oauth2.resource.preferTokenInfo:${OAUTH2_RESOURCE_PREFERTOKENINFO:true}}")
	protected static class UserInfoTokenServicesConfiguration {

		@Autowired
		private ResourceServerProperties sso;

		@Autowired
		@Qualifier("oauth2RestTemplate")
		private OAuth2RestOperations restTemplate;

		@Bean
		@ConditionalOnMissingBean(ResourceServerTokenServices.class)
		public UserInfoTokenServices userInfoTokenServices() {
			return new UserInfoTokenServices(restTemplate, sso.getUserInfoUri(),
					sso.getClientId());
		}

	}

}
