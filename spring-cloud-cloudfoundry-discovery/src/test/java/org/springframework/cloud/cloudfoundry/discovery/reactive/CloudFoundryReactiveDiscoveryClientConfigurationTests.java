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
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.ReactiveCommonsClientAutoConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryClientHealthIndicator;
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
					assertThat(context).doesNotHaveBean("cloudFoundryHeartbeatSender");
					assertThat(context).doesNotHaveBean(ReactiveDiscoveryClient.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void shouldNotHaveDiscoveryClientsWhenReactiveDiscoveryDisabled() {
		contextRunner.withPropertyValues("spring.cloud.discovery.reactive.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean("cloudFoundryHeartbeatSender");
					assertThat(context).doesNotHaveBean(ReactiveDiscoveryClient.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void shouldNotHaveDiscoveryClientsWhenCloudFoundryDiscoveryDisabled() {
		contextRunner
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean("cloudFoundryHeartbeatSender");
					assertThat(context).doesNotHaveBean(ReactiveDiscoveryClient.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void shouldUseNativeDiscovery() {
		contextRunner
				.withConfiguration(AutoConfigurations
						.of(ReactiveCommonsClientAutoConfiguration.class))
				.run(context -> {
					assertThat(context)
							.hasSingleBean(CloudFoundryReactiveHeartbeatSender.class);
					assertThat(context).hasSingleBean(ReactiveDiscoveryClient.class);
					assertThat(context).hasBean("nativeCloudFoundryDiscoveryClient");
					assertThat(context)
							.hasSingleBean(ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void shouldUseDnsDiscovery() {
		contextRunner
				.withConfiguration(AutoConfigurations
						.of(ReactiveCommonsClientAutoConfiguration.class))
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
						"spring.cloud.cloudfoundry.discovery.use-container-ip=true")
				.run(context -> {
					assertThat(context)
							.hasSingleBean(CloudFoundryReactiveHeartbeatSender.class);
					assertThat(context).hasSingleBean(ReactiveDiscoveryClient.class);
					assertThat(context).hasBean("dnsBasedReactiveDiscoveryClient");
					assertThat(context)
							.hasSingleBean(ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void shouldUseAppServiceDiscovery() {
		contextRunner
				.withConfiguration(AutoConfigurations
						.of(ReactiveCommonsClientAutoConfiguration.class))
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
						"spring.cloud.cloudfoundry.discovery.use-container-ip=false")
				.run(context -> {
					assertThat(context)
							.hasSingleBean(CloudFoundryReactiveHeartbeatSender.class);
					assertThat(context).hasSingleBean(ReactiveDiscoveryClient.class);
					assertThat(context).hasBean("appServiceReactiveDiscoveryClient");
					assertThat(context)
							.hasSingleBean(ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void shouldUseCustomServiceDiscovery() {
		contextRunner
				.withConfiguration(AutoConfigurations
						.of(ReactiveCommonsClientAutoConfiguration.class))
				.withUserConfiguration(
						CustomCloudFoundryReactiveDiscoveryClientConfiguration.class)
				.run(context -> {
					assertThat(context)
							.hasSingleBean(CloudFoundryReactiveHeartbeatSender.class);
					assertThat(context).hasSingleBean(ReactiveDiscoveryClient.class);
					assertThat(context)
							.doesNotHaveBean("nativeCloudFoundryDiscoveryClient");
					assertThat(context)
							.doesNotHaveBean("dnsBasedReactiveDiscoveryClient");
					assertThat(context)
							.doesNotHaveBean("appServiceReactiveDiscoveryClient");
					assertThat(context)
							.hasSingleBean(ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void worksWithoutWebflux() {
		contextRunner
				.withClassLoader(
						new FilteredClassLoader("org.springframework.web.reactive"))
				.run(context -> {
					assertThat(context)
							.doesNotHaveBean(CloudFoundryReactiveHeartbeatSender.class);
					assertThat(context)
							.doesNotHaveBean(CloudFoundryReactiveDiscoveryClient.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
				});
	}

	@Test
	public void worksWithoutActuator() {
		contextRunner
				.withClassLoader(
						new FilteredClassLoader("org.springframework.boot.actuate"))
				.run(context -> {
					assertThat(context)
							.hasSingleBean(CloudFoundryReactiveHeartbeatSender.class);
					assertThat(context).hasSingleBean(ReactiveDiscoveryClient.class);
					assertThat(context).doesNotHaveBean(
							ReactiveDiscoveryClientHealthIndicator.class);
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

	@TestConfiguration
	static class CustomCloudFoundryReactiveDiscoveryClientConfiguration {

		@Bean
		public CloudFoundryReactiveDiscoveryClient customDiscoveryClient() {
			return new CloudFoundryReactiveDiscoveryClient() {
				@Override
				public String description() {
					return "Custom CF Reactive Discovery Client";
				}

				@Override
				public Flux<ServiceInstance> getInstances(String serviceId) {
					return Flux.empty();
				}

				@Override
				public Flux<String> getServices() {
					return Flux.empty();
				}
			};
		}

	}

}
