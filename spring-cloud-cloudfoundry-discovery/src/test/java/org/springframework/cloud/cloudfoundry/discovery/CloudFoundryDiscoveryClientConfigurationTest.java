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
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryDiscoveryClientConfiguration}.
 *
 * @author Toshiaki Maki
 */
public class CloudFoundryDiscoveryClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CloudFoundryDiscoveryClientConfiguration.class));

	@Test
	public void testDefault() {
		this.contextRunner.withUserConfiguration(CloudFoundryConfig.class).run((context) -> {
			DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
			assertThat(discoveryClient.getClass()).isEqualTo(CloudFoundryDiscoveryClient.class);
		});
	}

	@Test
	public void testUseDnsTrue() {
		this.contextRunner.withUserConfiguration(CloudFoundryConfig.class)
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true").run((context) -> {
					DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
					assertThat(discoveryClient.getClass()).isEqualTo(CloudFoundryAppServiceDiscoveryClient.class);
				});
	}

	@Test
	public void testUseDnsFalse() {
		this.contextRunner.withUserConfiguration(CloudFoundryConfig.class)
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=false").run((context) -> {
					DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
					assertThat(discoveryClient.getClass()).isEqualTo(CloudFoundryDiscoveryClient.class);
				});
	}

	@Test
	public void testUseContainerIpFalse() {
		this.contextRunner.withUserConfiguration(CloudFoundryConfig.class)
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
						"spring.cloud.cloudfoundry.discovery.use-container-ip=false")
				.run((context) -> {
					DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
					assertThat(discoveryClient.getClass()).isEqualTo(CloudFoundryAppServiceDiscoveryClient.class);
				});
	}

	@Test
	public void testUseContainerIpTrue() {
		this.contextRunner.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
				"spring.cloud.cloudfoundry.discovery.use-container-ip=true").run((context) -> {
					DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
					assertThat(discoveryClient.getClass()).isEqualTo(SimpleDnsBasedDiscoveryClient.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	public static class CloudFoundryConfig {

		@Bean
		public CloudFoundryOperations cloudFoundryOperations() {
			return Mockito.mock(CloudFoundryOperations.class);
		}

		@Bean
		public CloudFoundryService cloudFoundryService() {
			return Mockito.mock(CloudFoundryService.class);
		}

	}

}
