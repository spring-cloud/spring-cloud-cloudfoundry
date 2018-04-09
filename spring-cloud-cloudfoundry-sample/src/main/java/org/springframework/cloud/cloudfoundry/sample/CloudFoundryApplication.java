/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.cloudfoundry.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryClient;
import org.springframework.cloud.cloudfoundry.discovery.EnableCloudFoundryClient;
import org.springframework.context.annotation.Bean;

/**
 * This example uses the Spring Cloud {@code DiscoveryClient} abstraction to list all services
 * available to the application. It should deployed to a Cloud Foundry organization and space that
 * has other applications deployed to it.
 *
 * There is a sample file in the project root called {@code hi-service.groovy} which can be
 * deployed using the {@code spring} CLI and the {@code cf} CLI that works appropriately for
 * this example.
 *
 * Either modify this application's {@code application.yml} configuration file to provide credentials
 * and other information for the Cloud Foundry the app is deployed to, or set the following
 * environment variables on the application (using {@code cf set-env}):
 *
 * * {@code CF_USERNAME}
 * * {@code CF_PASSWORD}
 * * {@code CF_ORG}
 * * {@code CF_SPACE}
 *
 * @author Josh Long
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Scott Frederick
 */
@SpringBootApplication
@EnableCloudFoundryClient
public class CloudFoundryApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudFoundryApplication.class, args);
	}

	@Bean
	CommandLineRunner demo(CloudFoundryDiscoveryClient discoveryClient) {
		Log log = LogFactory.getLog(getClass());
		return args ->
				discoveryClient.getServices().forEach(svc -> {
					log.info("service = " + svc);
					discoveryClient.getInstances(svc).forEach(si -> log.info("\tinstance = " + si));
				});
	}
}
