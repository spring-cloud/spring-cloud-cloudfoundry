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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * This example assumes you've registered an application on <A href="http://cloudfoundry.org">Cloud Foundry</A>
 * named {@code hi-service} that responds with a String at {@code /hi/{name}}. There is a sample file in the project
 * root called {@code hi-service.groovy} which you can deploy using the {@code spring} CLI and the {@code cf} CLI that
 * works appropriately for this demonstration.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 * @author Spencer Gibb
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    private Log log = LogFactory.getLog(getClass());

    @Bean
    CommandLineRunner consume(final LoadBalancerClient loadBalancerClient,
                              final DiscoveryClient discoveryClient,
                              final HiServiceClient hiServiceClient,
                              final RestTemplate restTemplate) {

        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {

                // this demonstrates using the CF/Ribbon-aware RestTemplate interceptor
                log.info("=====================================");
                log.info("Hi: " + restTemplate.getForEntity("http://hi-service/hi/{name}", String.class, "Josh"));

                // this demonstrates using the Spring Cloud Commons DiscoveryClient abstraction
                log.info("=====================================");
                for (String svc : discoveryClient.getServices()) {
                    log.info("service = " + svc);
                    List<ServiceInstance> instances = discoveryClient.getInstances(svc);
                    for (ServiceInstance si : instances) {
                        log.info("\t" + ReflectionToStringBuilder.reflectionToString(si, ToStringStyle.MULTI_LINE_STYLE));
                    }
                }

                log.info("=====================================");
                log.info("local: ");
                log.info("\t" + ReflectionToStringBuilder.reflectionToString(
                        discoveryClient.getLocalServiceInstance(), ToStringStyle.MULTI_LINE_STYLE));

                // this demonstrates using a CF/Ribbon-aware Feign client
                log.info("=====================================");
                log.info("Hi:" + hiServiceClient.hi("Josh"));

                // this demonstrates using the Spring Cloud Commons LoadBalancerClient
                log.info("=====================================");
                ServiceInstance choose = loadBalancerClient.choose("hi-service");
                log.info("chose: " + '(' + choose.getServiceId() + ") " + choose.getHost() + ':' + choose.getPort());
            }
        };
    }
}


@FeignClient("hi-service")
interface HiServiceClient {

    @RequestMapping(value = "/hi/{name}", method = RequestMethod.GET)
    Map<String, Object> hi(@PathVariable("name") String name);

}

