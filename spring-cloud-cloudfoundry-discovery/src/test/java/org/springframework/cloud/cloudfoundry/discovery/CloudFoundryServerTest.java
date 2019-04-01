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

import com.netflix.loadbalancer.Server;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * @author <A href="mailto:josh@Joshlong.com">Josh Long</A>
 */
public class CloudFoundryServerTest {

	private CloudFoundryServer cloudFoundryServer;
	private List<String> urls = Arrays.asList("a-url.com", "b-url.com");
	private String serverName = "server-name";

	@Before
	public void setUp() {
		CloudApplication cloudApplication = mock(CloudApplication.class);
		given(cloudApplication.getUris()).willReturn(this.urls);
		given(cloudApplication.getName()).willReturn(this.serverName);
		given(cloudApplication.getRunningInstances()).willReturn(1);
		this.cloudFoundryServer = new CloudFoundryServer(cloudApplication);
	}

	@Test
	public void testProperConstruction() {
		Server.MetaInfo metaInfo = this.cloudFoundryServer.getMetaInfo();

		Assert.assertEquals(metaInfo.getAppName(), this.serverName);
		Assert.assertEquals(metaInfo.getServiceIdForDiscovery(), this.serverName);
		Assert.assertEquals(metaInfo.getInstanceId(), this.serverName);
		Assert.assertEquals(this.cloudFoundryServer.getHost(), this.urls.get(0));
	}
}
