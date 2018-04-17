package org.springframework.cloud.cloudfoundry.discovery;

import java.util.HashMap;
import java.util.List;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

/**
 * @author Toshiaki Maki
 */
public class CloudFoundryAppServiceDiscoveryClientTest {
	private CloudFoundryAppServiceDiscoveryClient discoveryClient;
	private CloudFoundryOperations cloudFoundryOperations;
	private CloudFoundryService cloudFoundryService;

	@Before
	public void setUp() {
		this.cloudFoundryOperations = mock(CloudFoundryOperations.class);
		this.cloudFoundryService = mock(CloudFoundryService.class);
		this.discoveryClient = new CloudFoundryAppServiceDiscoveryClient(
				this.cloudFoundryOperations, this.cloudFoundryService,
				new CloudFoundryDiscoveryProperties());
	}

	@Test
	public void getInstancesOneInstance() {
		String serviceId = "billing";
		ApplicationDetail applicationDetail = ApplicationDetail.builder().id("billing1")
				.name("billing").instances(1).memoryLimit(1024).stack("cflinux2")
				.diskQuota(1024).requestedState("Running").runningInstances(1)
				.url("billing.apps.example.com", "billing.apps.internal").build();
		given(this.cloudFoundryService.getApplicationInstances(serviceId))
				.willReturn(Flux.just(Tuples.of(applicationDetail,
						InstanceDetail.builder().index("0").build())));
		List<ServiceInstance> instances = this.discoveryClient.getInstances(serviceId);

		assertThat(instances).hasSize(1);
		assertThat(instances.get(0)).isEqualTo(new DefaultServiceInstance(serviceId,
				"0.billing.apps.internal", 8080, false, new HashMap<String, String>() {
					{
						put("applicationId", "billing1");
						put("instanceId", "0");
					}
				}));
	}

	@Test
	public void getInstancesThreeInstance() {
		String serviceId = "billing";
		ApplicationDetail applicationDetail = ApplicationDetail.builder().id("billing-id")
				.name("billing").instances(3).memoryLimit(1024).stack("cflinux2")
				.diskQuota(1024).requestedState("Running").runningInstances(3)
				.url("billing.apps.example.com", "billing.apps.internal").build();
		given(this.cloudFoundryService.getApplicationInstances(serviceId))
				.willReturn(Flux.just(
						Tuples.of(applicationDetail,
								InstanceDetail.builder().index("0").build()),
						Tuples.of(applicationDetail,
								InstanceDetail.builder().index("1").build()),
						Tuples.of(applicationDetail,
								InstanceDetail.builder().index("2").build())));
		List<ServiceInstance> instances = this.discoveryClient.getInstances(serviceId);

		assertThat(instances).hasSize(3);
		assertThat(instances.get(0)).isEqualTo(new DefaultServiceInstance(serviceId,
				"0.billing.apps.internal", 8080, false, new HashMap<String, String>() {
					{
						put("applicationId", "billing-id");
						put("instanceId", "0");
					}
				}));
		assertThat(instances.get(1)).isEqualTo(new DefaultServiceInstance(serviceId,
				"1.billing.apps.internal", 8080, false, new HashMap<String, String>() {
					{
						put("applicationId", "billing-id");
						put("instanceId", "1");
					}
				}));
		assertThat(instances.get(2)).isEqualTo(new DefaultServiceInstance(serviceId,
				"2.billing.apps.internal", 8080, false, new HashMap<String, String>() {
					{
						put("applicationId", "billing-id");
						put("instanceId", "2");
					}
				}));
	}

	@Test
	public void getInstancesEmpty() {
		String serviceId = "billing";
		ApplicationDetail applicationDetail = ApplicationDetail.builder().id("billing1")
				.name("billing").instances(1).memoryLimit(1024).stack("cflinux2")
				.diskQuota(1024).requestedState("Running").runningInstances(1)
				.url("billing.apps.example.com").build();
		given(this.cloudFoundryService.getApplicationInstances(serviceId))
				.willReturn(Flux.just(Tuples.of(applicationDetail,
						InstanceDetail.builder().index("0").build())));
		List<ServiceInstance> instances = this.discoveryClient.getInstances(serviceId);

		assertThat(instances).isEmpty();
	}
}