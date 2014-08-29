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
package org.springframework.cloud.cloudfoundry.broker.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.community.servicebroker.config.BrokerApiVersionConfig;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.service.BeanCatalogService;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.cloud.cloudfoundry.broker.FreeServiceDefinitionFactory;
import org.springframework.cloud.cloudfoundry.broker.ServiceInstanceBindingRepository;
import org.springframework.cloud.cloudfoundry.broker.ServiceInstanceRepository;
import org.springframework.cloud.cloudfoundry.broker.simple.SimpleServiceInstanceBindingRepository;
import org.springframework.cloud.cloudfoundry.broker.simple.SimpleServiceInstanceBindingService;
import org.springframework.cloud.cloudfoundry.broker.simple.SimpleServiceInstanceRepository;
import org.springframework.cloud.cloudfoundry.broker.simple.SimpleServiceInstanceService;
import org.springframework.cloud.netflix.eureka.advice.PiggybackMethodInterceptor;
import org.springframework.cloud.netflix.eureka.event.EurekaRegistryAvailableEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.eureka.PeerAwareInstanceRegistry;
import com.netflix.eureka.lease.LeaseManager;

/**
 * Autoconfiguration providing simple service-broker endpoints and Netflix eureka server
 * features. The service-broker endpoints will be enabled if <a
 * href="https://github.com/cloudfoundry-community/spring-boot-cf-service-broker"
 * >spring-boot-cf-service-broker</a> is on the classpath. By default they just make the
 * current app into a service brooker for itself. The Eureka features will be added if <a
 * href="https://github.com/Netflix/eureka">ereka-core</a> is on the classpath. When
 * active any Eureka services registered using native Netflix APIs will be available as
 * Cloudfoundry services. They consist of an interceptor for the {@link LeaseManager} that
 * manages the {@link Catalog} of Cloudfoundry servvices.
 * 
 * 
 * @author Dave Syer
 *
 */
@Configuration
@ComponentScan(basePackages = { "org.cloudfoundry.community.servicebroker" }, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = BrokerApiVersionConfig.class),
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = BeanCatalogService.class) })
@ConditionalOnClass({ServiceInstanceRepository.class, BrokerApiVersionConfig.class })
@ConditionalOnWebApplication
public class ServiceBrokerAutoConfiguration {

	@Configuration
	@ConditionalOnMissingClass(name = "com.netflix.eureka.PeerAwareInstanceRegistry")
	@ConditionalOnMissingBean(CatalogService.class)
	protected static class CatalogConfiguration {

		@Autowired
		private BrokerProperties broker;

		@Bean
		public BeanCatalogService catalogService() {
			return new BeanCatalogService(catalog());
		}

		@Bean
		public Catalog catalog() {
			return new Catalog(Arrays.asList(serviceDefinition()));
		}

		@Bean
		@ConfigurationProperties("cloudfoundry.service.definition")
		public ServiceDefinition serviceDefinition() {
			return new FreeServiceDefinitionFactory(broker.getPrefix()).create(
					broker.getName(), broker.getDescription());
		}

		@Component
		@ConfigurationProperties("cloudfoundry.server.broker")
		public static class BrokerProperties {
			private String prefix;
			@Value("${spring.application.name:application}")
			private String name;
			private String description;

			public String getPrefix() {
				return prefix == null ? name + "-" : prefix;
			}

			public void setPrefix(String prefix) {
				this.prefix = prefix;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getDescription() {
				return description == null ? "Singleton service app" : description;
			}

			public void setDescription(String description) {
				this.description = description;
			}
		}

	}

	@Configuration
	@ConditionalOnClass(PeerAwareInstanceRegistry.class)
	protected static class EurekaCatalogConfiguration {

		@Bean
		public CatalogLeaseManager catalogLeaseManager(EurekaInstanceConfig config) {
			InstanceInfo info = new EurekaConfigBasedInstanceInfoProvider(config).get();
			return new CatalogLeaseManager(info);
		}

	}

	@Configuration
	@ConditionalOnClass(PeerAwareInstanceRegistry.class)
	protected static class Initializer implements
			ApplicationListener<EurekaRegistryAvailableEvent> {

		@Autowired
		private ApplicationContext applicationContext;

		@Autowired
		private CatalogLeaseManager leaseManager;

		@Override
		public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
			ProxyFactory factory = new ProxyFactory(
					PeerAwareInstanceRegistry.getInstance());
			factory.addAdvice(new PiggybackMethodInterceptor(leaseManager,
					LeaseManager.class));
			factory.setProxyTargetClass(true);
			Field field = ReflectionUtils.findField(PeerAwareInstanceRegistry.class,
					"instance");
			try {
				// Awful ugly hack to work around lack of DI in eureka
				field.setAccessible(true);
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
				ReflectionUtils.setField(field, null, factory.getProxy());
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot modify instance registry", e);
			}
		}

	}

	@Bean
	@ConditionalOnMissingBean(ServiceInstanceService.class)
	public SimpleServiceInstanceService serviceInstanceService(
			ServiceInstanceRepository repository) {
		return new SimpleServiceInstanceService(repository);
	}

	@Bean
	@ConditionalOnMissingBean(ServiceInstanceBindingService.class)
	public SimpleServiceInstanceBindingService serviceInstanceBindingService(
			CatalogService catalog, ServiceInstanceBindingRepository repository) {
		return new SimpleServiceInstanceBindingService(catalog, repository);
	}

	@Bean
	@ConditionalOnMissingBean(ServiceInstanceRepository.class)
	public SimpleServiceInstanceRepository serviceInstanceRepository(
			CatalogService catalog) {
		List<ServiceDefinition> definitions = catalog.getCatalog()
				.getServiceDefinitions();
		ServiceDefinition definition = definitions.iterator().next();
		return new SimpleServiceInstanceRepository(definition.getId());
	}

	@Bean
	@ConditionalOnMissingBean(ServiceInstanceBindingRepository.class)
	public SimpleServiceInstanceBindingRepository serviceInstanceBindingRepository(
			CatalogService catalog) {
		List<ServiceDefinition> definitions = catalog.getCatalog()
				.getServiceDefinitions();
		ServiceDefinition definition = definitions.iterator().next();
		return new SimpleServiceInstanceBindingRepository(definition.getId());
	}

}
