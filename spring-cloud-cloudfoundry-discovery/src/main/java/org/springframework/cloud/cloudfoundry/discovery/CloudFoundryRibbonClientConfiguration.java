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

package org.springframework.cloud.cloudfoundry.discovery;

import javax.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.netflix.ribbon.RibbonClientName;
import org.springframework.cloud.netflix.ribbon.RibbonUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ServerList;

/**
 * @author Josh Long
 */
@Configuration
public class CloudFoundryRibbonClientConfiguration {

	@RibbonClientName
	private String serviceId;

	public CloudFoundryRibbonClientConfiguration() {
	}

	public CloudFoundryRibbonClientConfiguration(String svcId) {
		this.serviceId = svcId;
	}

	@Bean
	@ConditionalOnMissingBean
	public ServerList<?> ribbonServerList(CloudFoundryService svc, IClientConfig config,
										  CloudFoundryDiscoveryProperties properties) {
		CloudFoundryServerList cloudFoundryServerList = new CloudFoundryServerList(svc, properties);
		cloudFoundryServerList.initWithNiwsConfig(config);
		return cloudFoundryServerList;
	}

	@PostConstruct
	public void postConstruct() {
		RibbonUtils.initializeRibbonDefaults(this.serviceId);
	}

}