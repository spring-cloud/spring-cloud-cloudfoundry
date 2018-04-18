
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
 * Discovery
 *
 * @see <a href=
 * "https://www.cloudfoundry.org/blog/polyglot-service-discovery-container-networking-cloud-foundry/">Polyglot
 * Service Discovery for Container Networking in Cloud Foundry</a>
 *
 * @author Toshiaki Maki
 */
public class SimpleDnsBasedDiscoveryClient implements DiscoveryClient {
	public static final String INTERNAL_DOMAIN = "apps.internal";
	private final Logger log = LoggerFactory
			.getLogger(SimpleDnsBasedDiscoveryClient.class);
	private final ServiceIdToHostnameConverter serviceIdToHostnameConverter;

	private static final ServiceIdToHostnameConverter DEFAULT_CONVERTER = serviceId -> serviceId
			+ "." + INTERNAL_DOMAIN;

	public SimpleDnsBasedDiscoveryClient(
			ServiceIdToHostnameConverter serviceIdToHostnameConverter) {
		this.serviceIdToHostnameConverter = serviceIdToHostnameConverter;
	}

	public SimpleDnsBasedDiscoveryClient() {
		this(DEFAULT_CONVERTER);
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
					DefaultServiceInstance serviceInstance = new DefaultServiceInstance(
							serviceId, address.getHostAddress(), 8080, false);
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