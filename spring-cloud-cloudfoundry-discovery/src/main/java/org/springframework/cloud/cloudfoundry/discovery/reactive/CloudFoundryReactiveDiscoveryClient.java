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

import java.util.HashMap;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryProperties;

/**
 * Cloud Foundry maintains a registry of running applications which we expose here as
 * CloudFoundryService instances.
 *
 * @author Tim Ysewyn
 */
public class CloudFoundryReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	private final CloudFoundryService cloudFoundryService;

	private final CloudFoundryOperations cloudFoundryOperations;

	private final CloudFoundryDiscoveryProperties properties;

	private final String description = "Cloud Foundry "
			+ ReactiveDiscoveryClient.class.getName() + " implementation";

	CloudFoundryReactiveDiscoveryClient(CloudFoundryOperations operations,
			CloudFoundryService svc, CloudFoundryDiscoveryProperties properties) {
		this.cloudFoundryService = svc;
		this.cloudFoundryOperations = operations;
		this.properties = properties;
	}

	@Override
	public String description() {
		return this.description;
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		return this.cloudFoundryService.getApplicationInstances(serviceId)
				.map(this::mapApplicationInstanceToServiceInstance);
	}

	@Override
	public Flux<String> getServices() {
		return this.cloudFoundryOperations.applications().list()
				.map(ApplicationSummary::getName);
	}

	@Override
	public int getOrder() {
		return this.properties.getOrder();
	}

	protected ServiceInstance mapApplicationInstanceToServiceInstance(
			Tuple2<ApplicationDetail, InstanceDetail> tuple) {
		ApplicationDetail applicationDetail = tuple.getT1();
		InstanceDetail instanceDetail = tuple.getT2();

		String applicationId = applicationDetail.getId();
		String applicationIndex = instanceDetail.getIndex();
		String instanceId = applicationId + "." + applicationIndex;
		String name = applicationDetail.getName();
		String url = applicationDetail.getUrls().size() > 0
				? applicationDetail.getUrls().get(0) : null;
		boolean secure = (url + "").toLowerCase().startsWith("https");

		HashMap<String, String> metadata = new HashMap<>();
		metadata.put("applicationId", applicationId);
		metadata.put("instanceId", applicationIndex);

		return new DefaultServiceInstance(instanceId, name, url, 80, secure, metadata);
	}

}
