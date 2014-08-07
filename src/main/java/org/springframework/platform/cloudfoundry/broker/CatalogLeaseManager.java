/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.platform.cloudfoundry.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.service.BeanCatalogService;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.springframework.platform.netflix.eureka.advice.LeaseManagerLite;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.ActionType;
import com.netflix.eureka.lease.LeaseManager;

/**
 * @author Dave Syer
 *
 */
public class CatalogLeaseManager implements LeaseManager<InstanceInfo>, LeaseManagerLite,
		CatalogService {

	private Map<String, ServiceDefinition> definitions = new HashMap<String, ServiceDefinition>();

	private List<ServiceDefinition> values = new ArrayList<ServiceDefinition>();

	public CatalogLeaseManager(InstanceInfo config) {
		register(config, false);
	}

	@Override
	public Catalog getCatalog() {
		return new Catalog(values);
	}

	@Override
	public ServiceDefinition getServiceDefinition(String serviceId) {
		return new BeanCatalogService(getCatalog()).getServiceDefinition(serviceId);
	}

	@Override
	public void register(InstanceInfo info, boolean isReplication) {
		register(info, 0, isReplication);
	}

	@Override
	public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
		if (!definitions.containsKey(info.getAppName())) {
			ServiceDefinition definition = getServiceDefinition(info);
			definitions.put(info.getAppName(), definition);
			values.add(definition);
		}
	}

	@Override
	public boolean cancel(String appName, String id, boolean isReplication) {
		ServiceDefinition definition = definitions.remove(appName);
		if (definition != null) {
			values.remove(definition);
		}
		return true;
	}

	@Override
	public boolean renew(String appName, String id, boolean isReplication) {
		if (definitions.containsKey(appName)) {
			definitions.get(appName).getMetadata()
					.put("timestamp", System.currentTimeMillis());
		}
		return true;
	}

	@Override
	public void evict() {
		Collection<ServiceDefinition> definitions = new ArrayList<ServiceDefinition>(
				this.definitions.values());
		for (ServiceDefinition definition : definitions) {
			InstanceInfo info = (InstanceInfo) definition.getMetadata().get("info");
			if (info.getActionType() == ActionType.DELETED) {
				cancel(info.getAppName(), info.getId(), false);
			}
		}
	}

	private ServiceDefinition getServiceDefinition(InstanceInfo info) {
		String name = info.getAppName().toLowerCase();
		ServiceDefinition definition = new FreeServiceDefinitionFactory("eureka-").create(name, "Eureka-brokered service");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("info", (Object) info);
		map.put("uri", info.getHomePageUrl());
		map.put("timestamp", System.currentTimeMillis());
		definition.setMetadata(map);
		return definition;
	}

}
