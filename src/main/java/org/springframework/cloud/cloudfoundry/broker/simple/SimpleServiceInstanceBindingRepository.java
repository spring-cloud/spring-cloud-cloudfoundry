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

import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.springframework.cloud.cloudfoundry.broker.ServiceInstanceBindingRepository;

/**
 * @author Dave Syer
 *
 */
public class SimpleServiceInstanceBindingRepository implements ServiceInstanceBindingRepository {

	private String serviceDefinitionId;

	public SimpleServiceInstanceBindingRepository(String serviceDefinitionId) {
		this.serviceDefinitionId = serviceDefinitionId;
	}

	@Override
	public ServiceInstanceBinding findOne(String id) {
		return new ServiceInstanceBinding(id, serviceDefinitionId, null, null, null);
	}

	@Override
	public void save(ServiceInstanceBinding instance) {
	}

	@Override
	public void delete(String id) {
	}

}
