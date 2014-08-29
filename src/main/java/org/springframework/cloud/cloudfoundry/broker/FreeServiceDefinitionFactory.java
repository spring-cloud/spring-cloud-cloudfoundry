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
package org.springframework.cloud.cloudfoundry.broker;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.util.DigestUtils;

/**
 * @author Dave Syer
 *
 */
public class FreeServiceDefinitionFactory {
	
	private final String prefix;
	
	public FreeServiceDefinitionFactory() {
		this("");
	}

	public FreeServiceDefinitionFactory(String prefix) {
		this.prefix = prefix;
	}

	public ServiceDefinition create(String name, String description) {
		return new ServiceDefinition(getId(name), name, description, true, getPlans(name));
	}

	private List<Plan> getPlans(String appName) {
		Plan plan = new Plan(getId(appName + "-free"), "free",
				"This is a default service plan.  All services are created equally.",
				getServiceDefinitionMetadata(appName), true);
		return new ArrayList<Plan>(Arrays.asList(plan));
	}

	private Map<String, Object> getServiceDefinitionMetadata(String appName) {
		Map<String, Object> sdMetadata = new HashMap<String, Object>();
		sdMetadata.put("displayName", appName + "-service");
		sdMetadata.put("longDescription", "Platform Service for " + appName);
		sdMetadata.put("providerDisplayName", "Pivotal");
		sdMetadata.put("documentationUrl", "https://github.com/spring-platform");
		sdMetadata.put("supportUrl", "https://github.com/spring-platform");
		return sdMetadata;
	}

	protected String getId(String name) {
		try {
			String id = DigestUtils.md5DigestAsHex((prefix + name).getBytes("UTF-8"));
			return id;
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException();
		}

	}

}
