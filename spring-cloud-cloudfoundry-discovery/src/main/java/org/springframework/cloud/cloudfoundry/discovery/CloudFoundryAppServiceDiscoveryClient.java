package org.springframework.cloud.cloudfoundry.discovery;

import java.util.HashMap;
import java.util.List;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;

/**
 *
 * Discovery Client implementation using Cloud Foundry's Native DNS based Service
 * Discovery
 *
 * @see <a href="https://github.com/cloudfoundry/cf-app-sd-release">CF App Service
 * Discovery Release</a>
 * @see <a href=
 * "https://www.cloudfoundry.org/blog/polyglot-service-discovery-container-networking-cloud-foundry/">Polyglot
 * Service Discovery for Container Networking in Cloud Foundry</a>
 *
 * @author Toshiaki Maki
 */
public class CloudFoundryAppServiceDiscoveryClient extends CloudFoundryDiscoveryClient {

	private static final String INTERNAL_DOMAIN = "apps.internal";

	CloudFoundryAppServiceDiscoveryClient(CloudFoundryOperations cloudFoundryOperations,
			CloudFoundryService svc, CloudFoundryDiscoveryProperties cloudFoundryDiscoveryProperties) {
		super(cloudFoundryOperations, svc, cloudFoundryDiscoveryProperties);
	}

	@Override
	public String description() {
		return "CF App Service Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		return getCloudFoundryService()
				.getApplicationInstances(serviceId)
                .filter(tuple -> tuple.getT1().getUrls().stream()
                        .anyMatch(this::isInternalDomain))
				.map(tuple -> {
					ApplicationDetail applicationDetail = tuple.getT1();
					InstanceDetail instanceDetail = tuple.getT2();
					String applicationId = applicationDetail.getId();
					String applicationIndex = instanceDetail.getIndex();
					String name = applicationDetail.getName();
					String url = applicationDetail.getUrls().stream()
							.filter(this::isInternalDomain)
                            .findFirst()
							.map(x -> instanceDetail.getIndex() + "." + x)
                            .get();
					HashMap<String, String> metadata = new HashMap<>();
					metadata.put("applicationId", applicationId);
					metadata.put("instanceId", applicationIndex);
					return (ServiceInstance) new DefaultServiceInstance(name, url, 8080,
							false, metadata);
				})
                .collectList()
				.block();
	}

	private boolean isInternalDomain(String url) {
		return url != null && url.endsWith(INTERNAL_DOMAIN);
	}
}
