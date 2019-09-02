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
		.withConfiguration(AutoConfigurations.of(
			MockedCloudFoundryConfiguration.class,
			CloudFoundryReactiveDiscoveryClientConfiguration.class));

	@Test
	public void shouldNotHaveDiscoveryClientsWhenDiscoveryDisabled() {
		contextRunner.withPropertyValues("spring.cloud.discovery.enabled=false")
			.run(context -> {
				assertThat(context.containsBean("cloudFoundryHeartbeatSender")).isFalse();
				assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient")).isFalse();
				assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient")).isFalse();
				assertThat(context.containsBean("appServiceReactiveDiscoveryClient")).isFalse();
			});

		contextRunner.withPropertyValues("spring.cloud.cloudfoundry.discovery.enabled=false")
			.run(context -> {
				assertThat(context.containsBean("cloudFoundryHeartbeatSender")).isFalse();
				assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient")).isFalse();
				assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient")).isFalse();
				assertThat(context.containsBean("appServiceReactiveDiscoveryClient")).isFalse();
			});
	}

	@Test
	public void shouldUseNativeDiscovery() {
		contextRunner
			.run(context -> {
				assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient")).isTrue();
				assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient")).isFalse();
				assertThat(context.containsBean("appServiceReactiveDiscoveryClient")).isFalse();
			});
	}

	@Test
	public void shouldUseDnsDiscovery() {
		contextRunner
			.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
				"spring.cloud.cloudfoundry.discovery.use-container-ip=true")
			.run(context -> {
				assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient")).isFalse();
				assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient")).isTrue();
				assertThat(context.containsBean("appServiceReactiveDiscoveryClient")).isFalse();
			});
	}

	@Test
	public void shouldUseAppServiceDiscovery() {
		contextRunner
			.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true",
				"spring.cloud.cloudfoundry.discovery.use-container-ip=false")
			.run(context -> {
				assertThat(context.containsBean("nativeCloudFoundryDiscoveryClient")).isFalse();
				assertThat(context.containsBean("dnsBasedReactiveDiscoveryClient")).isFalse();
				assertThat(context.containsBean("appServiceReactiveDiscoveryClient")).isTrue();
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
