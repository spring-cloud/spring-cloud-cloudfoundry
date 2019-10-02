/*
 * Copyright 2019-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.cloudfoundry.discovery.reactive;

import org.cloudfoundry.operations.CloudFoundryOperations;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryHealthIndicatorEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ReactiveCommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryClientHealthIndicator;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryProperties;
import org.springframework.cloud.cloudfoundry.discovery.ConditionalOnCloudFoundryDiscoveryEnabled;
import org.springframework.cloud.cloudfoundry.discovery.reactive.SimpleDnsBasedReactiveDiscoveryClient.ServiceIdToHostnameConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration related to service discovery when using Cloud Foundry.
 *
 * @author Tim Ysewyn
 */
@Configuration
@ConditionalOnClass(CloudFoundryOperations.class)
@ConditionalOnDiscoveryEnabled
@ConditionalOnReactiveDiscoveryEnabled
@ConditionalOnCloudFoundryDiscoveryEnabled
@EnableConfigurationProperties(CloudFoundryDiscoveryProperties.class)
@AutoConfigureAfter(ReactiveCompositeDiscoveryClientAutoConfiguration.class)
@AutoConfigureBefore(ReactiveCommonsClientAutoConfiguration.class)
public class CloudFoundryReactiveDiscoveryClientConfiguration {

	@Configuration
	@ConditionalOnProperty(value = "spring.cloud.cloudfoundry.discovery.use-dns",
			havingValue = "false", matchIfMissing = true)
	public static class CloudFoundryNativeReactiveDiscoveryClientConfig {

		@Bean
		@ConditionalOnMissingBean
		public CloudFoundryNativeReactiveDiscoveryClient nativeCloudFoundryDiscoveryClient(
				CloudFoundryOperations cf, CloudFoundryService svc,
				CloudFoundryDiscoveryProperties cloudFoundryDiscoveryProperties) {
			return new CloudFoundryNativeReactiveDiscoveryClient(cf, svc,
					cloudFoundryDiscoveryProperties);
		}

		@Bean
		@ConditionalOnClass(
				name = "org.springframework.boot.actuate.health.ReactiveHealthIndicator")
		@ConditionalOnDiscoveryHealthIndicatorEnabled
		public ReactiveDiscoveryClientHealthIndicator cloudFoundryReactiveDiscoveryClientHealthIndicator(
				CloudFoundryNativeReactiveDiscoveryClient client,
				DiscoveryClientHealthIndicatorProperties properties) {
			return new ReactiveDiscoveryClientHealthIndicator(client, properties);
		}

		@Bean
		public CloudFoundryReactiveHeartbeatSender cloudFoundryHeartbeatSender(
				CloudFoundryNativeReactiveDiscoveryClient client) {
			return new CloudFoundryReactiveHeartbeatSender(client);
		}

	}

	@Configuration
	@ConditionalOnProperty(value = "spring.cloud.cloudfoundry.discovery.use-dns",
			havingValue = "true")
	public static class DnsBasedCloudFoundryReactiveDiscoveryClientConfig {

		@Configuration
		@ConditionalOnProperty(
				value = "spring.cloud.cloudfoundry.discovery.use-container-ip",
				havingValue = "true")
		public static class CloudFoundrySimpleDnsBasedReactiveDiscoveryClientConfig {

			@Bean
			@ConditionalOnMissingBean
			public SimpleDnsBasedReactiveDiscoveryClient dnsBasedReactiveDiscoveryClient(
					ObjectProvider<ServiceIdToHostnameConverter> provider,
					CloudFoundryDiscoveryProperties properties) {
				ServiceIdToHostnameConverter converter = provider.getIfAvailable();
				return converter == null
						? new SimpleDnsBasedReactiveDiscoveryClient(properties)
						: new SimpleDnsBasedReactiveDiscoveryClient(converter);
			}

			@Bean
			@ConditionalOnClass(
					name = "org.springframework.boot.actuate.health.ReactiveHealthIndicator")
			@ConditionalOnDiscoveryHealthIndicatorEnabled
			public ReactiveDiscoveryClientHealthIndicator cloudFoundryReactiveDiscoveryClientHealthIndicator(
					SimpleDnsBasedReactiveDiscoveryClient client,
					DiscoveryClientHealthIndicatorProperties properties) {
				return new ReactiveDiscoveryClientHealthIndicator(client, properties);
			}

			@Bean
			public CloudFoundryReactiveHeartbeatSender cloudFoundryHeartbeatSender(
					SimpleDnsBasedReactiveDiscoveryClient client) {
				return new CloudFoundryReactiveHeartbeatSender(client);
			}

		}

		@Configuration
		@ConditionalOnProperty(
				value = "spring.cloud.cloudfoundry.discovery.use-container-ip",
				havingValue = "false", matchIfMissing = true)
		public static class CloudFoundryAppServiceReactiveDiscoveryClientConfig {

			@Bean
			@ConditionalOnMissingBean
			public CloudFoundryAppServiceReactiveDiscoveryClient appServiceReactiveDiscoveryClient(
					CloudFoundryOperations cf, CloudFoundryService svc,
					CloudFoundryDiscoveryProperties properties) {
				return new CloudFoundryAppServiceReactiveDiscoveryClient(cf, svc,
						properties);
			}

			@Bean
			@ConditionalOnClass(
					name = "org.springframework.boot.actuate.health.ReactiveHealthIndicator")
			@ConditionalOnDiscoveryHealthIndicatorEnabled
			public ReactiveDiscoveryClientHealthIndicator cloudFoundryReactiveDiscoveryClientHealthIndicator(
					CloudFoundryAppServiceReactiveDiscoveryClient client,
					DiscoveryClientHealthIndicatorProperties properties) {
				return new ReactiveDiscoveryClientHealthIndicator(client, properties);
			}

			@Bean
			public CloudFoundryReactiveHeartbeatSender cloudFoundryHeartbeatSender(
					CloudFoundryAppServiceReactiveDiscoveryClient client) {
				return new CloudFoundryReactiveHeartbeatSender(client);
			}

		}

	}

}
