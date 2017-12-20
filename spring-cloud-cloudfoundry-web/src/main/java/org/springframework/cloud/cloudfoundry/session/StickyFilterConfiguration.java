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
package org.springframework.cloud.cloudfoundry.session;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Dave Syer
 */
@Configuration
@PropertySource("spring-cloud-cloudfoundry.properties")
public class StickyFilterConfiguration {

	@Value("${spring.cloud.cloudfoundry.web.cookie}")
	private String cookie;

	@Bean
	public FilterRegistrationBean<?> stickyCloudFoundryFilter() {
		FilterRegistrationBean<Filter> filter = new FilterRegistrationBean<Filter>();
		filter.setOrder(Ordered.LOWEST_PRECEDENCE);
		filter.setFilter(new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request,
					HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
				if (!response.containsHeader("Set-Cookie")) {
					response.addCookie(new Cookie("JSESSIONID", cookie));
				}
				filterChain.doFilter(request, response);
			}
		});
		return filter;
	}

}
