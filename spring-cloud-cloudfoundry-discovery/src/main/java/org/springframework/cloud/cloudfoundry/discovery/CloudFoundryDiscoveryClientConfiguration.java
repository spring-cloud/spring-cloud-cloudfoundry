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

import java.net.MalformedURLException;
import java.net.URI;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass(CloudFoundryClient.class)
@ConditionalOnProperty(value = "spring.cloud.cloudfoundry.discovery.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CloudFoundryDiscoveryProperties.class)
public class CloudFoundryDiscoveryClientConfiguration {

	@Autowired
	private CloudFoundryDiscoveryProperties discovery;

	@Bean
	@ConditionalOnMissingBean(CloudCredentials.class)
	public CloudCredentials cloudCredentials() {
		return new CloudCredentials(this.discovery.getUsername(),
				this.discovery.getPassword());
	}

	@Bean
	@ConditionalOnMissingBean(CloudFoundryClient.class)
	public CloudFoundryClient cloudFoundryClient(CloudCredentials cc)
			throws MalformedURLException {
		CloudFoundryClient cloudFoundryClient;
		if (StringUtils.hasText(this.discovery.getOrg()) && StringUtils.hasText(this.discovery.getSpace())) {
			cloudFoundryClient = new CloudFoundryClient(cc,
					URI.create(this.discovery.getUrl()).toURL(), this.discovery.getOrg(),
					this.discovery.getSpace());
		}
		else {
			cloudFoundryClient = new CloudFoundryClient(cc,
					URI.create(this.discovery.getUrl()).toURL());
		}
		cloudFoundryClient.login();
		return cloudFoundryClient;
	}

	@Bean
	@ConditionalOnMissingBean(CloudFoundryDiscoveryClient.class)
	public CloudFoundryDiscoveryClient cloudFoundryDiscoveryClient(
			CloudFoundryClient cloudFoundryClient, Environment environment) {
		return new CloudFoundryDiscoveryClient(cloudFoundryClient, environment);
	}

	@Bean
	public CloudFoundryHeartbeatSender cloudFoundryHeartbeatSender(CloudFoundryDiscoveryClient client) {
		return new CloudFoundryHeartbeatSender(client);
	}

}