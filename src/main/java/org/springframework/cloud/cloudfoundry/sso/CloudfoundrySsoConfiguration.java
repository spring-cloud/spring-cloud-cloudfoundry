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
package org.springframework.cloud.cloudfoundry.sso;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
@Configuration
@ConditionalOnExpression("'${cloudfoundry.sso.clientId:${vcap.services.sso.credentials.clientId:}}'!=''")
@ConditionalOnClass({ EnableOAuth2Client.class, SecurityProperties.class })
@ConditionalOnWebApplication
@EnableOAuth2Client
@EnableConfigurationProperties(CloudfoundrySsoProperties.class)
public class CloudfoundrySsoConfiguration {

	@Autowired
	private CloudfoundrySsoProperties sso;

	@Resource
	@Qualifier("accessTokenRequest")
	private AccessTokenRequest accessTokenRequest;

	@Bean
	public FilterRegistrationBean oauth2ClientFilterRegistration(
			OAuth2ClientContextFilter filter) {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(filter);
		registration.setOrder(0);
		return registration;
	}

	@Bean
	public OAuth2ProtectedResourceDetails remote() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		// set up resource details, OAuth2 URLs etc.
		details.setClientId(sso.getClientId());
		details.setClientSecret(sso.getClientSecret());
		details.setAccessTokenUri(sso.getTokenUri());
		details.setUserAuthorizationUri(sso.getAuthorizationUri());
		return details;
	}

	@Bean
	public OAuth2RestOperations restTemplate() {
		return new OAuth2RestTemplate(remote(), oauth2ClientContext());
	}

	@Bean
	@Scope(value = "session", proxyMode = ScopedProxyMode.INTERFACES)
	public OAuth2ClientContext oauth2ClientContext() {
		return new DefaultOAuth2ClientContext(accessTokenRequest);
	}

	@Configuration
	protected static class SsoSecurityConfigurer extends WebSecurityConfigurerAdapter
			implements Ordered {

		@Autowired
		private OAuth2ProtectedResourceDetails remote;

		@Autowired
		private CloudfoundrySsoProperties sso;

		@Autowired
		private OAuth2RestOperations restTemplate;

		private List<CloudfoundrySsoConfigurer> configurers = Collections.emptyList();

		@Override
		public int getOrder() {
			if (ClassUtils
					.isPresent(
							"org.springframework.boot.actuate.autoconfigure.ManagementServerProperties",
							null)) {
				return ManagementServerProperties.ACCESS_OVERRIDE_ORDER;
			}
			return SecurityProperties.ACCESS_OVERRIDE_ORDER;
		}

		/**
		 * @param configurers the configurers to set
		 */
		@Autowired(required = false)
		public void setConfigurers(List<CloudfoundrySsoConfigurer> configurers) {
			this.configurers = configurers;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {

			http.addFilterAfter(cloudfoundrySsoFilter(),
					AbstractPreAuthenticatedProcessingFilter.class);

			for (CloudfoundrySsoConfigurer configurer : configurers) {
				// Delegates can add authorizeRequests() here
				configurer.configure(http);
			}
			if (configurers.isEmpty()) {
				// Add anyRequest() last as a fall back. Spring Security would replace an
				// existing anyRequest() matcher with this one, so to avoid that we only
				// add it if the user hasn't configured anything.
				ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry requests = http
						.antMatcher("/**").authorizeRequests();
				if (!sso.getHome().isSecure()) {
					requests.antMatchers(sso.getHome().getPath()).permitAll();
				}
				requests.anyRequest().authenticated();
			}

			http.logout().logoutRequestMatcher(new AntPathRequestMatcher(sso.getLogoutPath()))
					.addLogoutHandler(logoutHandler());
			http.exceptionHandling().authenticationEntryPoint(
					new LoginUrlAuthenticationEntryPoint(sso.getLoginPath()));

		}

		protected OAuth2ClientAuthenticationProcessingFilter cloudfoundrySsoFilter() {
			OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(
					sso.getLoginPath());
			filter.setRestTemplate(restTemplate);
			filter.setTokenServices(tokenServices());
			return filter;
		}

		private ResourceServerTokenServices tokenServices() {
			RemoteTokenServices services = new RemoteTokenServices();
			services.setCheckTokenEndpointUrl(sso.getTokenInfoUri());
			services.setClientId(sso.getClientId());
			services.setClientSecret(sso.getClientSecret());
			return services;
		}

		private LogoutHandler logoutHandler() {
			LogoutHandler handler = new LogoutHandler() {
				@Override
				public void logout(HttpServletRequest request,
						HttpServletResponse response, Authentication authentication) {
					restTemplate.getOAuth2ClientContext().setAccessToken(null);
					String redirect = request.getRequestURL().toString()
							.replace(sso.getLogoutPath(), sso.getHome().getPath());
					try {
						response.sendRedirect(sso.getLogoutUri(redirect));
					}
					catch (IOException e) {
						throw new IllegalStateException("Cannot logout", e);
					}
				}
			};
			return handler;
		}

	}

}
