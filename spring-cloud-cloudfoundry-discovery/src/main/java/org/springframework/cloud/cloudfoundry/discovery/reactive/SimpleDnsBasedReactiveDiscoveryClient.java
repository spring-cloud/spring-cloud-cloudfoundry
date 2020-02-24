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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryProperties;

/**
 * Reactive Discovery Client implementation using Cloud Foundry's Native DNS based Service
 * Discovery.
 *
 * @author Tim Ysewyn
 * @see <a href=
 * "https://www.cloudfoundry.org/blog/polyglot-service-discovery-container-networking-cloud-foundry/">Polyglot
 * Service Discovery for Container Networking in Cloud Foundry</a>
 */
public class SimpleDnsBasedReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	private static final Logger log = LoggerFactory
			.getLogger(SimpleDnsBasedReactiveDiscoveryClient.class);

	private final ServiceIdToHostnameConverter serviceIdToHostnameConverter;

	private final CloudFoundryDiscoveryProperties properties;

	public SimpleDnsBasedReactiveDiscoveryClient(
			ServiceIdToHostnameConverter serviceIdToHostnameConverter) {
		this.serviceIdToHostnameConverter = serviceIdToHostnameConverter;
		// added for backward compatibility
		this.properties = new CloudFoundryDiscoveryProperties();
	}

	public SimpleDnsBasedReactiveDiscoveryClient(
			CloudFoundryDiscoveryProperties properties) {
		this(serviceId -> serviceId + "." + properties.getInternalDomain());
	}

	public SimpleDnsBasedReactiveDiscoveryClient(
			CloudFoundryDiscoveryProperties properties,
			ServiceIdToHostnameConverter serviceIdToHostnameConverter) {
		this.properties = properties;
		this.serviceIdToHostnameConverter = serviceIdToHostnameConverter == null
				? (serviceId -> serviceId + "." + this.properties.getInternalDomain())
				: serviceIdToHostnameConverter;
	}

	@Override
	public String description() {
		return "DNS Based CF Service Reactive Discovery Client";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		return Mono.justOrEmpty(serviceIdToHostnameConverter.toHostname(serviceId))
				.flatMapMany(getInetAddresses())
				.map(address -> new DefaultServiceInstance(serviceId,
						address.getHostAddress(),
						properties.getDefaultServerPort() != 80
								? properties.getDefaultServerPort()
								: (serviceIdToHostnameConverter.toHostname(serviceId)
										.endsWith(properties.getInternalDomain()) ? 8080
												: properties.getDefaultServerPort()),
						false));
	}

	private Function<String, Publisher<? extends InetAddress>> getInetAddresses() {
		return hostname -> {
			try {
				return Flux.fromArray(InetAddress.getAllByName(hostname));
			}
			catch (UnknownHostException e) {
				log.warn("{}", e.getMessage());
				return Flux.empty();
			}
		};
	}

	@Override
	public Flux<String> getServices() {
		log.warn("getServices is not supported");
		return Flux.empty();
	}

	@FunctionalInterface
	public interface ServiceIdToHostnameConverter {

		String toHostname(String serviceId);

	}

}
