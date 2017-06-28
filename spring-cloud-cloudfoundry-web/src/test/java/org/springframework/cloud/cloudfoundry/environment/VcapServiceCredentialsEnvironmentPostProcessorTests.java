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
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.cloudfoundry.environment.VcapServiceCredentialsEnvironmentPostProcessor.STRING_OBJECT_MAP;

/**
 * @author Dave Syer
 *
 */
public class VcapServiceCredentialsEnvironmentPostProcessorTests {

	private VcapServiceCredentialsEnvironmentPostProcessor listener = new VcapServiceCredentialsEnvironmentPostProcessor();

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@Test
	public void noop() {
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		Map<String, Object> properties = Binder.get(environment)
				.bind("security.oauth2", STRING_OBJECT_MAP).orElseGet(Collections::emptyMap);
		assertTrue(properties.isEmpty());
	}

	@Test
	public void addClientId() {
		TestPropertyValues.of("vcap.services.sso.credentials.clientId:foo").applyTo(this.environment);
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		assertEquals("foo", this.environment
				.resolvePlaceholders("${security.oauth2.client.client-id}"));
	}

	@Test
	public void addClientIdUnderscores() {
		TestPropertyValues.of("vcap.services.sso.credentials.client-id:foo").applyTo(this.environment);
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		assertEquals("foo", this.environment
				.resolvePlaceholders("${security.oauth2.client.client-id}"));
	}

	@Test
	public void addTokenUri() {
		TestPropertyValues.of( "vcap.services.sso.credentials.accessTokenUri:http://example.com").applyTo(this.environment);
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		assertEquals("http://example.com", this.environment
				.resolvePlaceholders("${security.oauth2.client.access-token-uri}"));
	}

	@Test
	public void addTokenUriAuthDomain() {
		TestPropertyValues.of("vcap.services.sso.credentials.auth-domain:http://example.com").applyTo(this.environment);
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		assertEquals("http://example.com/oauth/token", this.environment
				.resolvePlaceholders("${security.oauth2.client.access-token-uri}"));
	}

	@Test
	public void addUserInfoUri() {
		TestPropertyValues.of( "vcap.services.sso.credentials.userInfoUri:http://example.com").applyTo(this.environment);
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		assertEquals("http://example.com", this.environment
				.resolvePlaceholders("${security.oauth2.resource.user-info-uri}"));
	}

	@Test
	public void addServiceId() {
		TestPropertyValues.of("vcap.services.my.credentials.accessTokenUri:http://example.com",
				"security.oauth2.sso.serviceId:my").applyTo(this.environment);
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		assertEquals("http://example.com", this.environment
				.resolvePlaceholders("${security.oauth2.client.access-token-uri}"));
	}

	@Test
	public void addJwtKeyUri() {
		TestPropertyValues.of("vcap.services.sso.credentials.keyUri:http://example.com").applyTo(this.environment);
		this.listener.postProcessEnvironment(this.environment, new SpringApplication());
		assertEquals("http://example.com", this.environment
				.resolvePlaceholders("${security.oauth2.resource.jwt.key-uri}"));
	}

}
