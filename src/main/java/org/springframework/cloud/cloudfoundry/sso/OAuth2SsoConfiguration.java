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
import java.util.Arrays;
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
import org.springframework.cloud.cloudfoundry.oauth2.ResourceServerTokenServicesConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
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
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * @author Dave Syer
 *
 */
@Configuration
@ConditionalOnExpression("'${oauth2.sso.clientId:${vcap.services.sso.credentials.clientId:}}'!=''")
@ConditionalOnClass({ EnableOAuth2Client.class, SecurityProperties.class })
@ConditionalOnWebApplication
@EnableOAuth2Client
@EnableConfigurationProperties(OAuth2SsoProperties.class)
@Import(ResourceServerTokenServicesConfiguration.class)
public class OAuth2SsoConfiguration {

	@Autowired
	private OAuth2SsoProperties sso;

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
	public OAuth2ProtectedResourceDetails oauth2RemoteResource() {
		AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
		// set up resource details, OAuth2 URLs etc.
		details.setClientId(sso.getClientId());
		details.setClientSecret(sso.getClientSecret());
		details.setAccessTokenUri(sso.getTokenUri());
		details.setUserAuthorizationUri(sso.getAuthorizationUri());
		details.setClientAuthenticationScheme(sso.getAuthenticationScheme());
		return details;
	}

	@Bean
	public OAuth2RestOperations oauth2RestTemplate() {
		OAuth2RestTemplate template = new OAuth2RestTemplate(oauth2RemoteResource(),
				oauth2ClientContext());
		template.setInterceptors(Arrays
				.<ClientHttpRequestInterceptor> asList(new ClientHttpRequestInterceptor() {
					@Override
					public ClientHttpResponse intercept(HttpRequest request, byte[] body,
							ClientHttpRequestExecution execution) throws IOException {
						request.getHeaders().setAccept(
								Arrays.asList(MediaType.APPLICATION_JSON));
						return execution.execute(request, body);
					}
				}));
		AuthorizationCodeAccessTokenProvider accessTokenProvider = new AuthorizationCodeAccessTokenProvider();
		accessTokenProvider.setTokenRequestEnhancer(new RequestEnhancer() {
			@Override
			public void enhance(AccessTokenRequest request,
					OAuth2ProtectedResourceDetails resource,
					MultiValueMap<String, String> form, HttpHeaders headers) {
				headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			}
		});
		template.setAccessTokenProvider(accessTokenProvider);
		return template;
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
		private OAuth2SsoProperties sso;

		@Autowired
		private ResourceServerTokenServices tokenServices;

		@Autowired
		@Qualifier("oauth2RestTemplate")
		private OAuth2RestOperations restTemplate;

		private List<OAuth2SsoConfigurer> configurers = Collections.emptyList();

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
		public void setConfigurers(List<OAuth2SsoConfigurer> configurers) {
			this.configurers = configurers;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {

			http.addFilterAfter(cloudfoundrySsoFilter(),
					AbstractPreAuthenticatedProcessingFilter.class);

			for (OAuth2SsoConfigurer configurer : configurers) {
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

			http.logout()
					.logoutRequestMatcher(new AntPathRequestMatcher(sso.getLogoutPath()))
					.addLogoutHandler(logoutHandler());
			http.exceptionHandling().authenticationEntryPoint(
					new LoginUrlAuthenticationEntryPoint(sso.getLoginPath()));

		}

		protected OAuth2ClientAuthenticationProcessingFilter cloudfoundrySsoFilter() {
			OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(
					sso.getLoginPath());
			filter.setRestTemplate(restTemplate);
			filter.setTokenServices(tokenServices);
			return filter;
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
