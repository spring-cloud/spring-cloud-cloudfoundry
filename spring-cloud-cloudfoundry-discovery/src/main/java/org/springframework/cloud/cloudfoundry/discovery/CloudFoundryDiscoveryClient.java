/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;

/**
 * Cloud Foundry maintains a registry of running applications which we expose here as
 * CloudFoundryService instances.
 *
 * @author Josh Long
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
public class CloudFoundryDiscoveryClient implements DiscoveryClient {

	private final CloudFoundryService cloudFoundryService;

	private final CloudFoundryOperations cloudFoundryOperations;

	private final CloudFoundryDiscoveryProperties properties;

	private final String description = "Cloud Foundry " + DiscoveryClient.class.getName() + " implementation";

	CloudFoundryDiscoveryClient(CloudFoundryOperations cloudFoundryOperations, CloudFoundryService svc,
			CloudFoundryDiscoveryProperties properties) {
		this.cloudFoundryService = svc;
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.properties = properties;
	}

	@Override
	public String description() {
		return this.description;
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		return this.cloudFoundryService.getApplicationInstances(serviceId).map(tuple -> {
			ApplicationDetail applicationDetail = tuple.getT1();
			InstanceDetail instanceDetail = tuple.getT2();

			String applicationId = applicationDetail.getId();
			String applicationIndex = instanceDetail.getIndex();
			String instanceId = applicationId + "." + applicationIndex;
			String name = applicationDetail.getName();
			String url = applicationDetail.getUrls().size() > 0 ? applicationDetail.getUrls().get(0) : null;
			boolean secure = (url + "").toLowerCase().startsWith("https");

			HashMap<String, String> metadata = new HashMap<>();
			metadata.put("applicationId", applicationId);
			metadata.put("instanceId", applicationIndex);

			return (ServiceInstance) new DefaultServiceInstance(instanceId, name, url, secure ? 443 : 80, secure,
					metadata);
		}).collectList().blockOptional().orElse(new ArrayList<>());
	}

	@Override
	public List<String> getServices() {
		return this.cloudFoundryOperations.applications().list().map(ApplicationSummary::getName).collectList()
				.blockOptional().orElse(new ArrayList<>());
	}

	@Override
	public int getOrder() {
		return this.properties.getOrder();
	}

	CloudFoundryService getCloudFoundryService() {
		return this.cloudFoundryService;
	}

}
