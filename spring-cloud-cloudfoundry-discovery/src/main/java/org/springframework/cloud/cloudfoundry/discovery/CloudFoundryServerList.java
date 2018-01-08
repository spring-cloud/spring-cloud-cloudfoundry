/*
 * Copyright 2013-2015 the original author or authors.
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

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Josh Long
 */
public class CloudFoundryServerList extends AbstractServerList<CloudFoundryServer> {

	private String serviceId;

	private final CloudFoundryService cloudFoundryService;

	CloudFoundryServerList(CloudFoundryService svc) {
		this.cloudFoundryService = svc;
	}

	@Override
	public void initWithNiwsConfig(IClientConfig iClientConfig) {
		this.serviceId = iClientConfig.getClientName();
	}

	@Override
	public List<CloudFoundryServer> getInitialListOfServers() {
		return this.cloudFoundryServers();
	}

	@Override
	public List<CloudFoundryServer> getUpdatedListOfServers() {
		return this.cloudFoundryServers();
	}

	private List<CloudFoundryServer> cloudFoundryServers() {
		return cloudFoundryService
				.getApplicationInstances(this.serviceId)
				.map(tpl -> new CloudFoundryServer(tpl.getT1().getName(), tpl.getT1().getUrls().get(0), 80))
				.collectList()
				.blockOptional()
				.orElse(new ArrayList<>());
	}
}