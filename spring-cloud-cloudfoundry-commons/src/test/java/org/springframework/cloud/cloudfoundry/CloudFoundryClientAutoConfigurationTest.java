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

import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.routing.RoutingClient;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudFoundryClientAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CloudFoundryClientAutoConfiguration.class));

	private final static String SPRING_CLOUD_PROPERTIES[] = {
			"spring.cloud.cloudfoundry.username",
			"spring.cloud.cloudfoundry.password",
	};

	private static boolean requiredPropertiesSet() {
		for (String k : SPRING_CLOUD_PROPERTIES) {
			if (System.getenv(envVarFromProperty(k)) == null) {
				return false;
			}
		}
		return true;
	}

	private static String envVarFromProperty(String propertyName) {
		return propertyName
				.replaceAll("\\.", "_")
				.toUpperCase();
	}

	@Test
	public void autoConfiguresBeansWithAllProperties() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.cloudfoundry.username=user",
						"spring.cloud.cloudfoundry.password=secret",
						"spring.cloud.cloudfoundry.org=myorg",
						"spring.cloud.cloudfoundry.space=myspace")
				.run((context) -> {
					assertCloudFoundryClientBeansPresent(context);

					DefaultCloudFoundryOperations operations = context.getBean(DefaultCloudFoundryOperations.class);
					assertThat(operations.getOrganization()).isEqualTo("myorg");
					assertThat(operations.getSpace()).isEqualTo("myspace");
				});
	}

	@Test
	public void autoConfiguresBeansWithMinimalProperties() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.cloudfoundry.username=user",
						"spring.cloud.cloudfoundry.password=secret")
				.run((context) -> {
					assertCloudFoundryClientBeansPresent(context);

					DefaultCloudFoundryOperations operations = context.getBean(DefaultCloudFoundryOperations.class);
					assertThat(operations.getOrganization()).isNullOrEmpty();
					assertThat(operations.getSpace()).isNullOrEmpty();
				});
	}

	@Test
	public void organizationsRetrievedWithUserProvidedProperties() {
		Assume.assumeTrue(requiredPropertiesSet());

		this.contextRunner
				.run((context) -> {
					assertCloudFoundryClientBeansPresent(context);
					CloudFoundryOperations operations = context.getBean(CloudFoundryOperations.class);

					OrganizationSummary summary = operations
							.organizations()
							.list()
							.blockFirst();

					assertThat(summary).isNotNull();
					assertThat(summary.getId()).isNotEmpty();
					assertThat(summary.getName()).isNotEmpty();
				});
	}

	private void assertCloudFoundryClientBeansPresent(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(ReactorCloudFoundryClient.class);
		assertThat(context).hasSingleBean(DefaultCloudFoundryOperations.class);
		assertThat(context).hasSingleBean(DefaultConnectionContext.class);
		assertThat(context).hasSingleBean(DopplerClient.class);
		assertThat(context).hasSingleBean(RoutingClient.class);
		assertThat(context).hasSingleBean(PasswordGrantTokenProvider.class);
		assertThat(context).hasSingleBean(ReactorUaaClient.class);
	}
}