/*
 * Copyright 2013-2015 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

/**
 * @author <A href="mailto:josh@Joshlong.com">Josh Long</A>
 */
public class CloudFoundryDiscoveryClientTest {

	private final Log log = LogFactory.getLog(getClass());

	private CloudFoundryDiscoveryClient cloudFoundryDiscoveryClient;

	private CloudApplication cloudApplication;

	private String hiServiceServiceId = "hi-service";

	private CloudFoundryClient cloudFoundryClient;

	private CloudApplication fakeCloudApplication(String name, String... uri) {
		CloudApplication cloudApplication = mock(CloudApplication.class);
		given(cloudApplication.getName()).willReturn(name);
		given(cloudApplication.getUris()).willReturn(Arrays.asList(uri));
		return cloudApplication;
	}

	@Before
	public void setUp() {
		this.cloudFoundryClient = mock(CloudFoundryClient.class);
		Environment environment = mock(Environment.class);

		given(environment.getProperty("VCAP_APPLICATION"))
				.willReturn(
						"{\"limits\":{\"mem\":1024,\"disk\":1024,\"fds\":16384},\"application_version\":"
								+ "\"36eff082-96d6-498f-8214-508fda72ba65\",\"application_name\":\""
								+ this.hiServiceServiceId
								+ "\",\"application_uris\""
								+ ":[\""
								+ this.hiServiceServiceId
								+ ".cfapps.io\"],\"version\":\"36eff082-96d6-498f-8214-508fda72ba65\",\"name\":"
								+ "\"hi-service\",\"space_name\":\"joshlong\",\"space_id\":\"e0cd969c-3461-41ae-abde-4e11bb5acbd1\","
								+ "\"uris\":[\"hi-service.cfapps.io\"],\"users\":null,\"application_id\":\"af350f7c-88c4-4e35-a04e-698a1dbc7354\","
								+ "\"instance_id\":\"e4843ca23bd947b28e6d4cb3f9b92cbb\",\"instance_index\":0,\"host\":\"0.0.0.0\",\"port\":61590,"
								+ "\"started_at\":\"2015-05-07 20:00:10 +0000\",\"started_at_timestamp\":1431028810,\"start\":\"2015-05-07 20:00:10 +0000\","
								+ "\"state_timestamp\":1431028810}");

		List<CloudApplication> cloudApplications = new ArrayList<>();
		cloudApplications.add(fakeCloudApplication(this.hiServiceServiceId,
				"hi-service.cfapps.io", "hi-service-1.cfapps.io"));
		cloudApplications.add(fakeCloudApplication("config-service",
				"conf-service.cfapps.io", "conf-service-1.cfapps.io"));

		given(this.cloudFoundryClient.getApplications()).willReturn(cloudApplications);

		this.cloudApplication = cloudApplications.get(0);
		given(this.cloudFoundryClient.getApplication(this.hiServiceServiceId))
				.willReturn(this.cloudApplication);

		given(this.cloudFoundryClient.getApplication(this.hiServiceServiceId))
				.willReturn(this.cloudApplication);

		InstanceInfo instanceInfo = mock(InstanceInfo.class);
		InstancesInfo instancesInfo = mock(InstancesInfo.class);
		given(instancesInfo.getInstances()).willReturn(
				Collections.singletonList(instanceInfo));
		given(instanceInfo.getState()).willReturn(InstanceState.RUNNING);

		given(this.cloudFoundryClient.getApplicationInstances(this.cloudApplication))
				.willReturn(instancesInfo);

		this.cloudFoundryDiscoveryClient = new CloudFoundryDiscoveryClient(
				this.cloudFoundryClient, environment);
	}

	@Test
	public void testServiceResolution() {
		List<String> serviceNames = this.cloudFoundryDiscoveryClient.getServices();

		Assert.assertTrue("there should be one registered service.",
				serviceNames.contains(this.hiServiceServiceId));

		for (String serviceName : serviceNames) {
			this.log.debug("\t discovered serviceName: " + serviceName);
		}
	}

	@Test
	public void testInstances() {
		List<ServiceInstance> instances = this.cloudFoundryDiscoveryClient
				.getInstances(this.hiServiceServiceId);
		assertEquals("Wrong instances: " + instances, 1, instances.size());
	}

	@Test
	public void testInstancesNotAvailable() {
		given(this.cloudFoundryClient.getApplication(this.hiServiceServiceId)).willThrow(new RuntimeException("Planned"));
		List<ServiceInstance> instances = this.cloudFoundryDiscoveryClient
				.getInstances(this.hiServiceServiceId);
		assertEquals("Wrong instances: " + instances, 0, instances.size());
	}

	@Test
	public void testLocalServiceInstanceRunning() {

		given(this.cloudFoundryClient.getApplication("application"))
				.willReturn(this.cloudApplication);
		InstanceInfo instanceInfo = mock(InstanceInfo.class);
		InstancesInfo instancesInfo = mock(InstancesInfo.class);
		given(instancesInfo.getInstances()).willReturn(
				Collections.singletonList(instanceInfo));
		given(instanceInfo.getState()).willReturn(InstanceState.RUNNING);

		given(this.cloudFoundryClient.getApplicationInstances(this.cloudApplication))
				.willReturn(instancesInfo);

		ServiceInstance localServiceInstance = this.cloudFoundryDiscoveryClient
				.getLocalServiceInstance();
		assertTrue(localServiceInstance.getHost().contains("hi-service.cfapps.io"));
		assertTrue(localServiceInstance.getServiceId().equals(this.hiServiceServiceId));
		assertEquals(localServiceInstance.getPort(), 80);
	}

	@Test
	public void testLocalServiceInstanceNotRunning() {

		InstanceInfo instanceInfo = mock(InstanceInfo.class);
		InstancesInfo instancesInfo = mock(InstancesInfo.class);
		given(instancesInfo.getInstances()).willReturn(
				Collections.singletonList(instanceInfo));
		given(instanceInfo.getState()).willReturn(InstanceState.CRASHED);

		given(this.cloudFoundryClient.getApplicationInstances(this.cloudApplication))
				.willReturn(instancesInfo);

		ServiceInstance localServiceInstance = this.cloudFoundryDiscoveryClient
				.getLocalServiceInstance();
		assertNull(localServiceInstance);
	}

	@Test
	public void testLocalServiceInstanceNotFound() {

		given(this.cloudFoundryClient.getApplicationInstances(this.cloudApplication))
				.willThrow(new CloudFoundryException(HttpStatus.NOT_FOUND));

		ServiceInstance localServiceInstance = this.cloudFoundryDiscoveryClient
				.getLocalServiceInstance();
		assertNull(localServiceInstance);
	}

}