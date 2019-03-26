/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.cloudfoundry.discovery;

import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Josh Long
 */
public class CloudFoundryDiscoveryClientTest {

	private final Log log = LogFactory.getLog(getClass());

	private CloudFoundryDiscoveryClient cloudFoundryDiscoveryClient;

	private String hiServiceServiceId = "hi-service";

	private CloudFoundryOperations ops;

	private CloudFoundryService svc;

	@Before
	public void setUp() {
		this.ops = mock(CloudFoundryOperations.class);
		this.svc = mock(CloudFoundryService.class);
		this.cloudFoundryDiscoveryClient = new CloudFoundryDiscoveryClient(this.ops,
				this.svc, new CloudFoundryDiscoveryProperties());
	}

	@Test
	public void testServiceResolution() {
		Applications apps = mock(Applications.class);
		ApplicationSummary s = ApplicationSummary.builder()
				.id(UUID.randomUUID().toString()).instances(2).memoryLimit(1024)
				.requestedState("requestedState").diskQuota(1024)
				.name(this.hiServiceServiceId).runningInstances(2).build();
		Mockito.when(apps.list()).thenReturn(Flux.just(s));
		Mockito.when(this.ops.applications()).thenReturn(apps);
		List<String> serviceNames = this.cloudFoundryDiscoveryClient.getServices();
		assertThat(serviceNames.contains(this.hiServiceServiceId))
				.as("there should be one registered service.").isTrue();
		serviceNames.forEach(serviceName -> this.log
				.debug("\t discovered serviceName: " + serviceName));
	}

	@Test
	public void testInstances() {
		ApplicationDetail applicationDetail = ApplicationDetail.builder().instances(2)
				.name("my-app").stack("stack").memoryLimit(1024).id("id")
				.requestedState("requestedState").runningInstances(2)
				.url("https://my-app.cfapps.io").diskQuota(20).build();
		InstanceDetail instanceDetail = InstanceDetail.builder().index("0").build();
		Tuple2<ApplicationDetail, InstanceDetail> tuple2 = Tuples.of(applicationDetail,
				instanceDetail);
		Mockito.when(this.svc.getApplicationInstances(this.hiServiceServiceId))
				.thenReturn(Flux.just(tuple2));
		List<ServiceInstance> instances = this.cloudFoundryDiscoveryClient
				.getInstances(this.hiServiceServiceId);
		assertThat(instances.size()).as("Wrong instances: " + instances).isEqualTo(1);
		assertThat(instances.get(0).getInstanceId()).as("Wrong instance ID").isEqualTo("id.0");
	}

}
