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

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.netflix.ribbon.RibbonProperties;
import org.springframework.util.Assert;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;

/**
 * @author Josh Long
 */
public class CloudFoundryServerList extends AbstractServerList<CloudFoundryServer> {
	private final CloudFoundryService cloudFoundryService;
	private final CloudFoundryDiscoveryProperties properties;

	private IClientConfig clientConfig;
	private String serviceId;

	CloudFoundryServerList(CloudFoundryService svc, CloudFoundryDiscoveryProperties properties) {
		this.cloudFoundryService = svc;
		this.properties = properties;
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		this.clientConfig = clientConfig;
		this.serviceId = clientConfig.getClientName();
	}

	@Override
	public List<CloudFoundryServer> getInitialListOfServers() {
		return cloudFoundryServers();
	}

	@Override
	public List<CloudFoundryServer> getUpdatedListOfServers() {
		return cloudFoundryServers();
	}

	private List<CloudFoundryServer> cloudFoundryServers() {
		Assert.notNull(this.clientConfig, "clientConfig may not be null");

		RibbonProperties ribbon = RibbonProperties.from(clientConfig);

		Boolean secure = ribbon.getSecure();
		Integer securePort = ribbon.getSecurePort();
		Integer nonSecurePort = ribbon.getPort();

		final int port;
		if (secure != null && secure && securePort != null) {
			port = securePort;
		} else if (nonSecurePort != null) {
			port = nonSecurePort;
		} else {
			port = this.properties.getDefaultServerPort();
		}

		return cloudFoundryService
				.getApplicationInstances(this.serviceId)
				.map(tpl -> new CloudFoundryServer(tpl.getT1().getName(), tpl.getT1().getUrls().get(0), port))
				.collectList()
				.blockOptional()
				.orElse(new ArrayList<>());
	}

	/** for testing */ String getServiceId() {
		return serviceId;
	}
}