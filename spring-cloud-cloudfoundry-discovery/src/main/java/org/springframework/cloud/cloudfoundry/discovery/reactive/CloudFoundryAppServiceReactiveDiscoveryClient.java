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

import org.cloudfoundry.operations.CloudFoundryOperations;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryProperties;

/**
 * Discovery Client implementation using Cloud Foundry's Native DNS based Service
 * Discovery.
 *
 * @author Tim Ysewyn
 * @see <a href="https://github.com/cloudfoundry/cf-app-sd-release">CF App Service
 * Discovery Release</a>
 * @see <a href=
 * "https://www.cloudfoundry.org/blog/polyglot-service-discovery-container-networking-cloud-foundry/">Polyglot
 * Service Discovery for Container Networking in Cloud Foundry</a>
 */
public class CloudFoundryAppServiceReactiveDiscoveryClient extends CloudFoundryReactiveDiscoveryClient {

	private static final String INTERNAL_DOMAIN = "apps.internal";

	private final CloudFoundryService cloudFoundryService;

	CloudFoundryAppServiceReactiveDiscoveryClient(CloudFoundryOperations cloudFoundryOperations,
			CloudFoundryService svc,
			CloudFoundryDiscoveryProperties cloudFoundryDiscoveryProperties) {
		super(cloudFoundryOperations, svc, cloudFoundryDiscoveryProperties);
		this.cloudFoundryService = svc;
	}

	@Override
	public String description() {
		return "CF App Reactive Service Discovery Client";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		return cloudFoundryService
				.getApplicationInstances(serviceId).filter(tuple -> tuple.getT1()
						.getUrls().stream().anyMatch(this::isInternalDomain))
				.map(this::mapApplicationInstanceToServiceInstance);
	}

	private boolean isInternalDomain(String url) {
		return url != null && url.endsWith(INTERNAL_DOMAIN);
	}

}
