package org.springframework.cloud.cloudfoundry.discovery.reactive;

import java.util.UUID;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 */
@ExtendWith(MockitoExtension.class)
class CloudFoundryReactiveDiscoveryClientTests {

//	private final SimpleDiscoveryProperties.SimpleServiceInstance service1Inst1 = new SimpleDiscoveryProperties.SimpleServiceInstance(
//		URI.create("http://host1:8080"));
//
//	private final SimpleDiscoveryProperties.SimpleServiceInstance service1Inst2 = new SimpleDiscoveryProperties.SimpleServiceInstance(
//		URI.create("https://host2:8443"));

	@Mock
	private CloudFoundryOperations operations;

	@Mock
	private CloudFoundryService svc;

	@Mock
	private CloudFoundryDiscoveryProperties properties;

	@InjectMocks
	private CloudFoundryReactiveDiscoveryClient client;

	@Test
	public void verifyDefaults() {
		when(properties.getOrder()).thenReturn(0);
		assertThat(client.description()).isEqualTo("Cloud Foundry org.springframework.cloud.client.discovery.ReactiveDiscoveryClient implementation");
		assertThat(client.getOrder()).isEqualTo(0);
	}

	@Test
	public void shouldReturnFluxOfServices() {
		Applications apps = mock(Applications.class);
		when(operations.applications()).thenReturn(apps);
		ApplicationSummary summary = ApplicationSummary.builder()
			.id(UUID.randomUUID().toString()).instances(1).memoryLimit(1024)
			.requestedState("requestedState").diskQuota(1024)
			.name("service").runningInstances(1).build();
		when(apps.list()).thenReturn(Flux.just(summary));
		Flux<String> services = this.client.getServices();
		StepVerifier.create(services).expectNext("service").expectComplete().verify();
	}

	@Test
	public void shouldReturnEmptyFluxForNonExistingService() {
		when(svc.getApplicationInstances("service")).thenReturn(Flux.empty());
		Flux<ServiceInstance> instances = this.client.getInstances("service");
		StepVerifier.create(instances).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void shouldReturnFluxOfServiceInstances() {
		ApplicationDetail applicationDetail = ApplicationDetail.builder()
			.id(UUID.randomUUID().toString()).stack("stack").instances(1).memoryLimit(1024)
			.requestedState("requestedState").diskQuota(1024)
			.name("service").runningInstances(1).build();
		InstanceDetail instanceDetail = InstanceDetail.builder().index("0").build();
		Tuple2<ApplicationDetail, InstanceDetail> instance = Tuples.of(applicationDetail,
			instanceDetail);
		when(this.svc.getApplicationInstances("service"))
			.thenReturn(Flux.just(instance));
		Flux<ServiceInstance> instances = this.client.getInstances("service");
		StepVerifier.create(instances).expectNextCount(1).expectComplete().verify();
	}

}
