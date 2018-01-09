/*
 * Copyright 2013-2018 the original author or authors.
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

import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * @author Josh Long
 */
public class CloudFoundryServerListTest {

	private CloudFoundryServerList cloudFoundryServerList;
	private String serviceId = "foo-service";

	@Before
	public void setUp() {
		IClientConfig iClientConfig = IClientConfig.Builder.newBuilder(this.serviceId)
				.withSecure(true)
				.build();
		iClientConfig.set(CommonClientConfigKey.SecurePort, 443);

		Tuple2<ApplicationDetail, InstanceDetail> tuple2 = getInstanceDetail();

		CloudFoundryService cfs = mock(CloudFoundryService.class);
		when(cfs.getApplicationInstances(this.serviceId)).thenReturn(Flux.just(tuple2));

		this.cloudFoundryServerList = new CloudFoundryServerList(cfs, new CloudFoundryDiscoveryProperties());
		this.cloudFoundryServerList.initWithNiwsConfig(iClientConfig);
	}

	private Tuple2<ApplicationDetail, InstanceDetail> getInstanceDetail() {
		// @formatter:off
		ApplicationDetail applicationDetail = ApplicationDetail
				.builder()
				.instances(2)
				.name("my-app")
				.stack("stack")
				.memoryLimit(1024)
				.id("id")
				.requestedState("requestedState")
				.runningInstances(2)
				.url("https://my-app.cfapps.io")
				.diskQuota(20)
				.build();

		InstanceDetail instanceDetail = InstanceDetail
				.builder()
				.index("0")
				.build();
		// @formatter:on

		return Tuples.of(applicationDetail, instanceDetail);
	}

	@Test
	public void testListOfServers() {
		List<CloudFoundryServer> initialListOfServers = this.cloudFoundryServerList.getInitialListOfServers();
		List<CloudFoundryServer> updatedListOfServers = this.cloudFoundryServerList.getUpdatedListOfServers();
		assertThat(initialListOfServers)
				.containsExactly(updatedListOfServers.toArray(new CloudFoundryServer[0]))
				.hasSize(1);

		CloudFoundryServer server = initialListOfServers.get(0);
		assertThat(server.getPort()).isEqualTo(443);
	}

	@Test
	public void testDefaultServerPort() {
		IClientConfig iClientConfig = mock(IClientConfig.class);
		when(iClientConfig.getClientName()).thenReturn(this.serviceId);

		Tuple2<ApplicationDetail, InstanceDetail> tuple2 = getInstanceDetail();

		CloudFoundryService cfs = mock(CloudFoundryService.class);
		when(cfs.getApplicationInstances(this.serviceId)).thenReturn(Flux.just(tuple2));

		CloudFoundryServerList serverList = new CloudFoundryServerList(cfs, new CloudFoundryDiscoveryProperties());
		serverList.initWithNiwsConfig(iClientConfig);

		CloudFoundryServer server = serverList.getInitialListOfServers().get(0);
		assertThat(server.getPort()).isEqualTo(80);
	}

	@Test
	public void testInit() {
		assertThat(this.cloudFoundryServerList.getServiceId()).isEqualTo(this.serviceId);
	}

}
