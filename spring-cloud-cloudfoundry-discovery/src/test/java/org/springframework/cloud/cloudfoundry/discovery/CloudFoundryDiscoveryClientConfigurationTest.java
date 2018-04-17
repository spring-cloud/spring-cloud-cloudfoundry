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
 * @author Toshiaki Maki
 */
public class CloudFoundryDiscoveryClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations
					.of(CloudFoundryDiscoveryClientConfiguration.class));

	@Test
	public void testDefault() {
		this.contextRunner.withUserConfiguration(CloudFoundryConfig.class)
				.run((context) -> {
					DiscoveryClient discoveryClient = context
							.getBean(DiscoveryClient.class);
					assertThat(discoveryClient.getClass())
							.isEqualTo(CloudFoundryDiscoveryClient.class);
				});
	}

	@Test
	public void testUseDnsTrue() {
		this.contextRunner.withUserConfiguration(CloudFoundryConfig.class)
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=true")
				.run((context) -> {
					DiscoveryClient discoveryClient = context
							.getBean(DiscoveryClient.class);
					assertThat(discoveryClient.getClass())
							.isEqualTo(CloudFoundryAppServiceDiscoveryClient.class);
				});
	}

	@Test
	public void testUseDnsFalse() {
		this.contextRunner.withUserConfiguration(CloudFoundryConfig.class)
				.withPropertyValues("spring.cloud.cloudfoundry.discovery.use-dns=false")
				.run((context) -> {
					DiscoveryClient discoveryClient = context
							.getBean(DiscoveryClient.class);
					assertThat(discoveryClient.getClass())
							.isEqualTo(CloudFoundryDiscoveryClient.class);
				});
	}

	@Configuration
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