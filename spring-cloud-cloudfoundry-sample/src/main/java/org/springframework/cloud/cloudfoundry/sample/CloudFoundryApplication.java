/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.cloudfoundry.sample;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryClient;
import org.springframework.cloud.cloudfoundry.discovery.EnableCloudFoundryClient;
import org.springframework.context.annotation.Bean;

/**
 * This example assumes you've registered an application on
 * <A href="http://cloudfoundry.org">Cloud Foundry</A> named {@code hi-service} that
 * responds with a String at {@code /hi/ name} . There is a sample file in the project
 * root called {@code hi-service.groovy} which you can deploy using the {@code spring} CLI
 * and the {@code cf} CLI that works appropriately for this demonstration.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 * @author Spencer Gibb
 * @author Dave Syer
 */
@SpringBootApplication
@EnableCloudFoundryClient
public class CloudFoundryApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudFoundryApplication.class, args);
	}

	private Log log = LogFactory.getLog(getClass());

	@Bean
	CommandLineRunner consume(final CloudFoundryDiscoveryClient discoveryClient) {

		return new CommandLineRunner() {
			@Override
			public void run(String... args) throws Exception {

				// this demonstrates using the Spring Cloud Commons DiscoveryClient
				// abstraction
				log.info("=====================================");
				for (String svc : discoveryClient.getServices()) {
					log.info("service = " + svc);
					List<ServiceInstance> instances = discoveryClient.getInstances(svc);
					for (ServiceInstance si : instances) {
						log.info("\t" + si);
					}
				}

				log.info("=====================================");
				log.info("local: ");
				log.info("\t" + discoveryClient.getLocalServiceInstance());

			}
		};
	}
}
