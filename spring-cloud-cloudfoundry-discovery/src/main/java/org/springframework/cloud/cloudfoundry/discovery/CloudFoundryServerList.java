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
import com.netflix.loadbalancer.AbstractServerList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.util.Collections;
import java.util.List;

/**
 * @author <A href="mailto:josh@joshlong.com">Josh Long</A>
 */
public class CloudFoundryServerList extends AbstractServerList<CloudFoundryServer> {

	private static final Log log = LogFactory.getLog(CloudFoundryServerList.class);

	protected String serviceId;

	private final CloudFoundryClient cloudFoundryClient;

	public CloudFoundryServerList(CloudFoundryClient cloudFoundryClient) {
		this.cloudFoundryClient = cloudFoundryClient;
	}

	@Override
	public void initWithNiwsConfig(IClientConfig iClientConfig) {
		this.serviceId = iClientConfig.getClientName();
	}

	@Override
	public List<CloudFoundryServer> getInitialListOfServers() {
		return this.cloudFoundryServers();
	}

	@Override
	public List<CloudFoundryServer> getUpdatedListOfServers() {
		return this.cloudFoundryServers();
	}

	protected List<CloudFoundryServer> cloudFoundryServers() {
		try {
			CloudApplication cloudApplications = this.cloudFoundryClient
					.getApplication(this.serviceId);
			return Collections.singletonList(new CloudFoundryServer(cloudApplications));
		}
		catch (Exception e) {
			log.warn("Cannot determine server list for " + serviceId + ": " + e.getClass() + "(" + e.getMessage() + ")");
			return Collections.emptyList();
		}
	}
}