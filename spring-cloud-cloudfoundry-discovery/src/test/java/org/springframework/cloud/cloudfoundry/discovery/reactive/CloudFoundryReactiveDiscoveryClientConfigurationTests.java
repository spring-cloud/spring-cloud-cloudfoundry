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
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Tim Ysewyn
 */
class CloudFoundryReactiveDiscoveryClientConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(MockedCloudFoundryConfiguration.class,
							CloudFoundryReactiveDiscoveryClientConfiguration.class));

	@Test
	public void shouldNotHaveDiscoveryClientsWhenDiscoveryDisabled() {
		contextRunner.withPropertyValues("spring.cloud.discovery.enabled=false")
				.run(context -> {
					assertThat(context.containsBean("cloudFoundryHeartbeatSender"))
							.isFalse();
					assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("appServiceReactiveDiscoveryClient"))
							.isFalse();
				});
	}

	@Test
	public void shouldNotHaveDiscoveryClientsWhenReactiveDiscoveryDisabled() {
		contextRunner.withPropertyValues("spring.cloud.discovery.reactive.enabled=false")
				.run(context -> {
					assertThat(context.containsBean("cloudFoundryHeartbeatSender"))
							.isFalse();
					assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("appServiceReactiveDiscoveryClient"))
							.isFalse();
				});
	}

	@Test
	public void shouldNotHaveDiscoveryClientsWhenCloudFoundryDiscoveryDisabled() {
		contextRunner
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.enabled=false")
				.run(context -> {
					assertThat(context.containsBean("cloudFoundryHeartbeatSender"))
							.isFalse();
					assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("appServiceReactiveDiscoveryClient"))
							.isFalse();
				});
	}

	@Test
	public void shouldUseNativeDiscovery() {
		contextRunner.run(context -> {
			assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient"))
					.isTrue();
			assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient")).isFalse();
			assertThat(context.containsBean("appServiceReactiveDiscoveryClient"))
					.isFalse();
		});
	}

	@Test
	public void shouldUseDnsDiscovery() {
		contextRunner
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
						"spring.cloud.cloudfoundry.discovery.use-container-ip=true")
				.run(context -> {
					assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient"))
							.isTrue();
					assertThat(context.containsBean("appServiceReactiveDiscoveryClient"))
							.isFalse();
				});
	}

	@Test
	public void shouldUseAppServiceDiscovery() {
		contextRunner
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
						"spring.cloud.cloudfoundry.discovery.use-container-ip=false")
				.run(context -> {
					assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient"))
							.isFalse();
					assertThat(context.containsBean("appServiceReactiveDiscoveryClient"))
							.isTrue();
				});
	}

	@TestConfiguration
	static class MockedCloudFoundryConfiguration {

		@Bean
		public CloudFoundryOperations mockedOperations() {
			return mock(CloudFoundryOperations.class);
		}

		@Bean
		public CloudFoundryService mockedService() {
			return mock(CloudFoundryService.class);
		}

	}

}
