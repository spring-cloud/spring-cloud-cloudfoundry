/*
 * Copyright 2013-2015 the original author or authors.
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

import com.netflix.client.config.IClientConfig;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <A href="mailto:josh@Joshlong.com">Josh Long</A>
 */
public class CloudFoundryServerListTest {

	private CloudFoundryServerList cloudFoundryServerList;
	private String serviceId = "foo-service";

	@Before
	public void setUp() {

		IClientConfig iClientConfig = mock(IClientConfig.class);
		when(iClientConfig.getClientName()).thenReturn(this.serviceId);

		CloudFoundryService cfs = mock(CloudFoundryService.class);
		ApplicationDetail applicationDetail = ApplicationDetail
				.builder()
				.instances(2)
				.name("my-app")
				.stack("stack")
				.memoryLimit(1024)
				.id("id")
				.requestedState("requestedState")
				.runningInstances(2)
				.url("http://my-app.cfapps.io")
				.diskQuota(20)
				.build();

		InstanceDetail instanceDetail = InstanceDetail
				.builder()
				.index("0")
				.build();

		Tuple2<ApplicationDetail, InstanceDetail> tuple2 = Tuples.of(applicationDetail, instanceDetail);
		Mockito.when(cfs.getApplicationInstances(this.serviceId)).thenReturn(Flux.just(tuple2));

		this.cloudFoundryServerList = new CloudFoundryServerList(cfs);
		this.cloudFoundryServerList.initWithNiwsConfig(iClientConfig);
	}

	@Test
	public void testListOfServers() {
		List<CloudFoundryServer> initialListOfServers = this.cloudFoundryServerList.getInitialListOfServers();
		List<CloudFoundryServer> updatedListOfServers = this.cloudFoundryServerList.getUpdatedListOfServers();
		Assert.assertEquals(updatedListOfServers, initialListOfServers);
		Assert.assertTrue(initialListOfServers.size() == 1);
	}

	@Test
	public void testInit() throws Exception {
		Field field = ReflectionUtils.findField(this.cloudFoundryServerList.getClass(), "serviceId");
		assert field != null;
		ReflectionUtils.makeAccessible(field);
		Assert.assertEquals(String.class.cast(field.get(this.cloudFoundryServerList)), this.serviceId);
	}

}
