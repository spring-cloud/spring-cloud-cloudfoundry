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

import java.util.UUID;

import org.cloudfoundry.operations.applications.ApplicationDetail;
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

import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 */
@ExtendWith(MockitoExtension.class)
class CloudFoundryAppServiceReactiveDiscoveryClientTests {

	@Mock
	private CloudFoundryService svc;

	@InjectMocks
	private CloudFoundryAppServiceReactiveDiscoveryClient client;

	@Test
	public void shouldReturnFluxOfServiceInstances() {
		ApplicationDetail appDetail1 = ApplicationDetail.builder().id(UUID.randomUUID().toString()).stack("stack")
				.instances(1).memoryLimit(1024).requestedState("requestedState").diskQuota(1024).name("service")
				.runningInstances(1).url("instance.apps.internal").build();
		Tuple2<ApplicationDetail, InstanceDetail> instance1 = Tuples.of(appDetail1,
				InstanceDetail.builder().index("0").build());
		ApplicationDetail appDetail2 = ApplicationDetail.builder().id(UUID.randomUUID().toString()).stack("stack")
				.instances(1).memoryLimit(1024).requestedState("requestedState").diskQuota(1024).name("service")
				.runningInstances(1).url("instance.apps.not.internal").build();
		Tuple2<ApplicationDetail, InstanceDetail> instance2 = Tuples.of(appDetail2,
				InstanceDetail.builder().index("0").build());
		when(this.svc.getApplicationInstances("service")).thenReturn(Flux.just(instance1, instance2));
		Flux<ServiceInstance> instances = this.client.getInstances("service");
		StepVerifier.create(instances).expectNextCount(1).expectComplete().verify();
	}

}
