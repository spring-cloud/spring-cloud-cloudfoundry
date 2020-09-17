/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.cloudfoundry.discovery;

import org.cloudfoundry.operations.CloudFoundryOperations;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.cloudfoundry.discovery.SimpleDnsBasedDiscoveryClient.ServiceIdToHostnameConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Josh Long
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CloudFoundryOperations.class)
@ConditionalOnDiscoveryEnabled
@ConditionalOnBlockingDiscoveryEnabled
@ConditionalOnCloudFoundryDiscoveryEnabled
@EnableConfigurationProperties(CloudFoundryDiscoveryProperties.class)
public class CloudFoundryDiscoveryClientConfiguration {

	@Bean
	@ConditionalOnBean(CloudFoundryDiscoveryClient.class)
	public CloudFoundryHeartbeatSender cloudFoundryHeartbeatSender(CloudFoundryDiscoveryClient client) {
		return new CloudFoundryHeartbeatSender(client);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "spring.cloud.cloudfoundry.discovery.use-dns", havingValue = "false",
			matchIfMissing = true)
	public static class CloudFoundryDiscoveryClientConfig {

		@Bean
		@ConditionalOnMissingBean(DiscoveryClient.class)
		public CloudFoundryDiscoveryClient cloudFoundryDiscoveryClient(CloudFoundryOperations cf,
				CloudFoundryService svc, CloudFoundryDiscoveryProperties cloudFoundryDiscoveryProperties) {
			return new CloudFoundryDiscoveryClient(cf, svc, cloudFoundryDiscoveryProperties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "spring.cloud.cloudfoundry.discovery.use-dns", havingValue = "true")
	public static class DnsBasedCloudFoundryDiscoveryClientConfig {

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.cloudfoundry.discovery.use-container-ip", havingValue = "true")
		@ConditionalOnMissingBean(DiscoveryClient.class)
		public SimpleDnsBasedDiscoveryClient discoveryClient(ObjectProvider<ServiceIdToHostnameConverter> provider,
				CloudFoundryDiscoveryProperties properties) {
			ServiceIdToHostnameConverter converter = provider.getIfAvailable();
			return converter == null ? new SimpleDnsBasedDiscoveryClient(properties)
					: new SimpleDnsBasedDiscoveryClient(properties, converter);
		}

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.cloudfoundry.discovery.use-container-ip", havingValue = "false",
				matchIfMissing = true)
		@ConditionalOnMissingBean(DiscoveryClient.class)
		public CloudFoundryAppServiceDiscoveryClient cloudFoundryDiscoveryClient(CloudFoundryOperations cf,
				CloudFoundryService svc, CloudFoundryDiscoveryProperties properties) {
			return new CloudFoundryAppServiceDiscoveryClient(cf, svc, properties);
		}

	}

}
