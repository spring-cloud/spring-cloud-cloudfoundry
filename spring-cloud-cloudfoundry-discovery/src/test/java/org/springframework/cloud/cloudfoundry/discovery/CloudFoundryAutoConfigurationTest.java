/*
 * Copyright 2013-2015 the original author or authors.
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

import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author <A href= "josh@joshlong.com">Josh Long</A>
 */
public class CloudFoundryAutoConfigurationTest {

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {

		String hiServiceServiceId = "foo-service";

		Object vcapAppl = "{\"limits\":{\"mem\":1024,\"disk\":1024,\"fds\":16384},\"application_version\":"
				+ "\"36eff082-96d6-498f-8214-508fda72ba65\",\"application_name\":\""
				+ hiServiceServiceId
				+ "\",\"application_uris\""
				+ ":[\""
				+ hiServiceServiceId
				+ ".cfapps.io\"],\"version\":\"36eff082-96d6-498f-8214-508fda72ba65\",\"name\":"
				+ "\"hi-service\",\"space_name\":\"joshlong\",\"space_id\":\"e0cd969c-3461-41ae-abde-4e11bb5acbd1\","
				+ "\"uris\":[\"hi-service.cfapps.io\"],\"users\":null,\"application_id\":\"af350f7c-88c4-4e35-a04e-698a1dbc7354\","
				+ "\"instance_id\":\"e4843ca23bd947b28e6d4cb3f9b92cbb\",\"instance_index\":0,\"host\":\"0.0.0.0\",\"port\":61590,"
				+ "\"started_at\":\"2015-05-07 20:00:10 +0000\",\"started_at_timestamp\":1431028810,\"start\":\"2015-05-07 20:00:10 +0000\","
				+ "\"state_timestamp\":1431028810}";

		this.context = new SpringApplicationBuilder()
				.properties("VCAP_APPLICATION:"+vcapAppl, "server.port=0")
				.sources(SimpleConfiguration.class).run();
	}

	@After
	public void after() throws Throwable {
		synchronized (this) {
			if (null != this.context)
				this.context.close();
		}
	}

	@Configuration
	@EnableDiscoveryClient
	@EnableFeignClients
	@EnableAutoConfiguration
	public static class SimpleConfiguration {

		@Bean
		CloudCredentials cloudCredentials() {
			return Mockito.mock(CloudCredentials.class);
		}

		@Bean
		CloudFoundryClient cloudFoundryClient() {
			return Mockito.mock(CloudFoundryClient.class);
		}

	}

	@Test
	public void contextLoaded() {
		LogFactory.getLog(getClass()).debug("contextLoad()");
		Assert.assertTrue(this.context.getBeansOfType(CloudFoundryDiscoveryClient.class)
				.size() > 0);
		Assert.assertTrue(this.context.getBeansOfType(
				CloudFoundryDiscoveryProperties.class).size() > 0);
		Assert.assertTrue(this.context.getBeansOfType(CloudFoundryClient.class).size() > 0);
	}

}
