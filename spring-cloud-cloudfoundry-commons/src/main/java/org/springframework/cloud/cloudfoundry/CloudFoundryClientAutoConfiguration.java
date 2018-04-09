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

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.routing.ReactorRoutingClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.routing.RoutingClient;
import org.cloudfoundry.uaa.UaaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Provides auto-configuration for the Reactor-based Cloud Foundry client v3.x.
 *
 * @author Josh Long
 * @author Ben Hale
 * @author Scott Frederick
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.cloud.cloudfoundry", name = {"username", "password"})
@ConditionalOnClass(name = {"reactor.core.publisher.Flux", "org.cloudfoundry.operations.DefaultCloudFoundryOperations",
		"org.cloudfoundry.reactor.client.ReactorCloudFoundryClient", "org.reactivestreams.Publisher"})
@EnableConfigurationProperties(CloudFoundryProperties.class)
public class CloudFoundryClientAutoConfiguration {

	private final CloudFoundryProperties cloudFoundryProperties;

	public CloudFoundryClientAutoConfiguration(CloudFoundryProperties cfp) {
		this.cloudFoundryProperties = cfp;
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public CloudFoundryService cloudFoundryService(CloudFoundryOperations cloudFoundryOperations) {
		return new CloudFoundryService(cloudFoundryOperations);
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public DefaultCloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient,
																DopplerClient dopplerClient,
																RoutingClient routingClient,
																UaaClient uaaClient) {
		String organization = this.cloudFoundryProperties.getOrg();
		String space = this.cloudFoundryProperties.getSpace();
		return DefaultCloudFoundryOperations
				.builder()
				.cloudFoundryClient(cloudFoundryClient)
				.dopplerClient(dopplerClient)
				.routingClient(routingClient)
				.uaaClient(uaaClient)
				.organization(organization)
				.space(space)
				.build();
	}
	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public DopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public RoutingClient routingClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorRoutingClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public DefaultConnectionContext connectionContext() {
		String apiHost = this.cloudFoundryProperties.getUrl();
		Boolean skipSslValidation = this.cloudFoundryProperties.isSkipSslValidation();

		return DefaultConnectionContext.builder()
				.apiHost(apiHost)
				.skipSslValidation(skipSslValidation)
				.build();
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public PasswordGrantTokenProvider tokenProvider() {
		String username = this.cloudFoundryProperties.getUsername();
		String password = this.cloudFoundryProperties.getPassword();
		return PasswordGrantTokenProvider.builder()
				.password(password)
				.username(username)
				.build();
	}
}


