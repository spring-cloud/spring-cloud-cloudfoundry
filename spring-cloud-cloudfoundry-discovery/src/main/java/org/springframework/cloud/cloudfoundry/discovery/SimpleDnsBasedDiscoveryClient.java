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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Discovery Client implementation using Cloud Foundry's Native DNS based Service
 * Discovery.
 *
 * @author Toshiaki Maki
 * @see <a href=
 * "https://www.cloudfoundry.org/blog/polyglot-service-discovery-container-networking-cloud-foundry/">Polyglot
 * Service Discovery for Container Networking in Cloud Foundry</a>
 */
public class SimpleDnsBasedDiscoveryClient implements DiscoveryClient {

	private static final Logger log = LoggerFactory.getLogger(SimpleDnsBasedDiscoveryClient.class);

	private final ServiceIdToHostnameConverter serviceIdToHostnameConverter;

	public SimpleDnsBasedDiscoveryClient(CloudFoundryDiscoveryProperties properties,
			ServiceIdToHostnameConverter serviceIdToHostnameConverter) {
		this.serviceIdToHostnameConverter = serviceIdToHostnameConverter;
	}

	public SimpleDnsBasedDiscoveryClient(CloudFoundryDiscoveryProperties properties) {
		this(properties, serviceId -> serviceId + "." + properties.getInternalDomain());
	}

	@Override
	public String description() {
		return "DNS Based CF Service Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		String hostname = this.serviceIdToHostnameConverter.toHostname(serviceId);
		try {
			List<ServiceInstance> serviceInstances = new ArrayList<>();
			InetAddress[] addresses = InetAddress.getAllByName(hostname);
			if (addresses != null) {
				for (InetAddress address : addresses) {
					DefaultServiceInstance serviceInstance = new DefaultServiceInstance(null, serviceId,
							address.getHostAddress(), 8080, false);
					serviceInstances.add(serviceInstance);
				}
			}
			return serviceInstances;
		}
		catch (UnknownHostException e) {
			log.warn("{}", e.getMessage());
			return Collections.emptyList();
		}
	}

	@Override
	public List<String> getServices() {
		log.warn("getServices is not supported");
		return Collections.emptyList();
	}

	@FunctionalInterface
	public interface ServiceIdToHostnameConverter {

		String toHostname(String serviceId);

	}

}
