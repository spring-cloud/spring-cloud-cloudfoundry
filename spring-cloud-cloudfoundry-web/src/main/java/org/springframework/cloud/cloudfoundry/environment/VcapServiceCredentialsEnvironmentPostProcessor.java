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
package org.springframework.cloud.cloudfoundry.environment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 *
 */
public class VcapServiceCredentialsEnvironmentPostProcessor
		implements EnvironmentPostProcessor, Ordered {

	static final Bindable<Map<String, Object>> STRING_OBJECT_MAP = Bindable
			.mapOf(String.class, Object.class);

	// After VcapEnvironmentPostProcessor and ConfigFileEnvironmentPostProcessor so
	// values here can
	// use those ones
	private int order = ConfigFileApplicationListener.DEFAULT_ORDER + 1;

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		Binder.get(environment).bind("vcap.services", STRING_OBJECT_MAP)
				.orElseGet(Collections::emptyMap);
		if (!hasChildProperties(environment, "vcap.services")) {
			return;
		}
		Map<String, Object> source = new HashMap<>();
		String serviceId;
		if (hasChildProperties(environment, "security.oauth2.resource")) {
			serviceId = environment.getProperty("security.oauth2.resource.service-id",
					"resource");
		}
		else {
			serviceId = environment.getProperty("security.oauth2.sso.service-id", "sso");
		}
		String authDomain = environment
				.getProperty("vcap.services." + serviceId + ".credentials.auth-domain");
		if (authDomain != null) {
			source.put("security.oauth2.resource.user-info-uri",
					authDomain + "/userinfo");
			source.put("security.oauth2.resource.jwt.key-uri", authDomain + "/token_key");
			source.put("security.oauth2.client.access-token-uri",
					authDomain + "/oauth/token");
			source.put("security.oauth2.client.user-authorization-uri",
					authDomain + "/oauth/authorize");
		}
		else {
			addProperty(source, environment, serviceId, "resource", "user-info-uri");
			addProperty(source, environment, serviceId, "resource", "token-info-uri");
			addProperty(source, environment, serviceId, "resource.jwt", "key-uri");
			addProperty(source, environment, serviceId, "resource", "key-value");
			addProperty(source, environment, serviceId, "client", "access-token-uri",
					"token-uri");
			addProperty(source, environment, serviceId, "client",
					"user-authorization-uri", "authorization-uri");
		}
		addProperty(source, environment, serviceId, "client", "client-id");
		addProperty(source, environment, serviceId, "client", "client-secret");
		addProperty(source, environment, serviceId, "client", "scope");
		String resourceId = environment
				.getProperty("vcap.services." + serviceId + ".credentials.id", "");
		if (StringUtils.hasText(resourceId)) {
			source.put("security.oauth2.resource.id", resourceId);
		}
		environment.getPropertySources()
				.addLast(new MapPropertySource("cloudDefaultSecurityBindings", source));
	}

	private boolean hasChildProperties(ConfigurableEnvironment environment, String name) {
		Map<String, Object> properties = Binder.get(environment)
				.bind(name, STRING_OBJECT_MAP).orElseGet(Collections::emptyMap);
		return !properties.isEmpty();
	}

	private void addProperty(Map<String, Object> source, PropertyResolver resolver,
			String serviceId, String stem, String key, String... altKeys) {
		String value = resolve(resolver, serviceId, key);
		if (StringUtils.hasText(value)) {
			source.put("security.oauth2." + stem + "." + key, value);
			return;
		}
		for (String altKey : altKeys) {
			value = resolve(resolver, serviceId, altKey);
			if (StringUtils.hasText(value)) {
				source.put("security.oauth2." + stem + "." + key, value);
				return;
			}
		}
	}

	private String resolve(PropertyResolver resolver, String serviceId, String key) {
		return resolver.getProperty(
				String.format("vcap.services.%s.credentials.%s", serviceId, key), "");
	}

}
