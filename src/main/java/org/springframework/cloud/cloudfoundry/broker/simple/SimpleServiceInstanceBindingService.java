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
package org.springframework.cloud.cloudfoundry.broker.simple;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.cloudfoundry.broker.ServiceInstanceBindingRepository;
import org.springframework.stereotype.Service;

/**
 * Simple impl to bind services.
 * 
 * @author sgreenberg@gopivotal.com
 *
 */
@Service
public class SimpleServiceInstanceBindingService implements ServiceInstanceBindingService {

	private ServiceInstanceBindingRepository repository;
	private CatalogService catalog;
	@Value("${application.domain:cfapps.io}")
	private String applicationDomain;

	@Autowired
	public SimpleServiceInstanceBindingService(CatalogService catalog,
			ServiceInstanceBindingRepository repository) {
		this.catalog = catalog;
		this.repository = repository;
	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(String bindingId,
			ServiceInstance serviceInstance, String serviceId, String planId,
			String appGuid) throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		ServiceInstanceBinding binding = repository.findOne(bindingId);
		if (binding != null && binding.getAppGuid()!=null) {
			throw new ServiceInstanceBindingExistsException(binding);
		}

		binding = new ServiceInstanceBinding(bindingId, serviceInstance.getId(),
				getCredentials(serviceInstance, serviceId, planId, appGuid), null, appGuid);
		repository.save(binding);

		return binding;
	}

	protected Map<String, Object> getCredentials(ServiceInstance instance,
			String serviceId, String planId, String appGuid) {
		Map<String, Object> credentials = new HashMap<String, Object>();
		credentials.put("uri", findUriFromService(serviceId));
		credentials.put("domain", applicationDomain);
		return credentials;
	}

	protected String findUriFromService(String serviceId) {
		ServiceDefinition definition = catalog.getServiceDefinition(serviceId);
		if (definition != null && definition.getId().equals(serviceId)) {
			String uri = (String) definition.getMetadata().get("uri");
			if (uri != null) {
				return uri;
			}
			return "http://" + definition.getName() + "." + applicationDomain;
		}
		throw new IllegalStateException("Cannot locate service in catalog: " + serviceId);
	}

	@Override
	public ServiceInstanceBinding getServiceInstanceBinding(String id) {
		return repository.findOne(id);
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(String id)
			throws ServiceBrokerException {
		ServiceInstanceBinding binding = getServiceInstanceBinding(id);
		if (binding != null) {
			repository.delete(id);
		}
		return binding;
	}

}
