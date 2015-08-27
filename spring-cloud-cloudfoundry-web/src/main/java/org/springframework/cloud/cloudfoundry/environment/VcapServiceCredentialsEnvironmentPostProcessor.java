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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.config.ConfigFileEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 *
 */
public class VcapServiceCredentialsEnvironmentPostProcessor
implements EnvironmentPostProcessor, Ordered {

	// After VcapEnvironmentPostProcessor and ConfigFileEnvironmentPostProcessor so
	// values here can
	// use those ones
	private int order = ConfigFileEnvironmentPostProcessor.DEFAULT_ORDER + 1;

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment);
		Map<String, Object> properties = resolver.getSubProperties("vcap.services.");
		if (properties == null || properties.isEmpty()) {
			return;
		}
		Map<String, Object> source = new HashMap<String, Object>();
		String serviceId;
		if (!resolver.getSubProperties("security.oauth2.resource.").isEmpty()) {
			serviceId = resolver.getProperty("security.oauth2.resource.serviceId",
					"resource");
		}
		else {
			serviceId = resolver.getProperty("security.oauth2.sso.serviceId", "sso");
		}
		String authDomain = (String) properties
				.get(serviceId + ".credentials.auth_domain");
		if (authDomain != null) {
			source.put("security.oauth2.resource.userInfoUri",
					authDomain + "/userinfo");
			source.put("security.oauth2.resource.keyUri", authDomain + "/token_key");
			source.put("security.oauth2.resource.accessTokenUri",
					authDomain + "/oauth/token");
			source.put("security.oauth2.resource.authorizationUri",
					authDomain + "/oauth/authorization");
		}
		else {
			addProperty(source, resolver, serviceId, "resource", "userInfoUri");
			addProperty(source, resolver, serviceId, "resource", "tokenInfoUri");
			addProperty(source, resolver, serviceId, "resource", "keyUri");
			addProperty(source, resolver, serviceId, "resource", "keyValue");
			addProperty(source, resolver, serviceId, "client", "accessTokenUri", "tokenUri");
			addProperty(source, resolver, serviceId, "client", "authorizationUri");
		}
		addProperty(source, resolver, serviceId, "client", "clientId");
		addProperty(source, resolver, serviceId, "client", "clientSecret");
		addProperty(source, resolver, serviceId, "client", "scope");
		String resourceId = resolver
				.getProperty("vcap.services." + serviceId + ".credentials.id", "");
		if (StringUtils.hasText(resourceId)) {
			source.put("security.oauth2.resource.id", resourceId);
		}
		environment.getPropertySources()
		.addLast(new MapPropertySource("cloudDefaultSecurityBindings", source));
	}

	private void addProperty(Map<String, Object> source,
			RelaxedPropertyResolver resolver, String serviceId, String stem, String key, String... altKeys) {
		String value = resolve(resolver, serviceId, key);
		if (StringUtils.hasText(value)) {
			source.put("security.oauth2."+stem+"." + key, value);
			return;
		}
		for (String altKey : altKeys) {
			value = resolve(resolver, serviceId, altKey);
			if (StringUtils.hasText(value)) {
				source.put("security.oauth2."+stem+"." + key, value);
				return;
			}
		}
	}

	private String resolve(RelaxedPropertyResolver resolver, String serviceId,
			String key) {
		return resolver.getProperty(
				String.format("vcap.services.%s.credentials.%s", serviceId, key),
				"");
	}

}
