package org.springframework.cloud.cloudfoundry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.assertj.core.api.Assertions;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.routing.RoutingClient;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudFoundryClientAutoConfigurationTest {

	@SpringBootApplication
	public static class MyConfig {
	}

	private static String envVarFromProperty(String propertyName) {
		return propertyName
				.replaceAll("\\.", "_")
				.toUpperCase();
	}

	private static String CONFIG_KEYS[] = {"spring.cloud.cloudfoundry.username",
			"spring.cloud.cloudfoundry.password", "spring.cloud.cloudfoundry.space", "spring.cloud.cloudfoundry.org"};

	private static Map<String, Object> defaultConfig() {
		Map<String, Object> kvs = new HashMap<>();
		for (String k : CONFIG_KEYS)
			kvs.put(k, System.getenv(envVarFromProperty(k)));
		return kvs;
	}

	private static boolean configExists() {
		for (String k : CONFIG_KEYS)
			if (System.getenv(envVarFromProperty(k)) == null)
				return false;
		return true;
	}

	@Test
	public void producesAllBeans() {

		Assume.assumeTrue(configExists());

		ApplicationContext ctx = new SpringApplicationBuilder()
				.sources(MyConfig.class)
				.properties(defaultConfig())
				.run();

		Class[] tags = {ReactorCloudFoundryClient.class, DefaultCloudFoundryOperations.class,
				DefaultConnectionContext.class, DopplerClient.class, RoutingClient.class, PasswordGrantTokenProvider.class,
				ReactorUaaClient.class};
		for (Class<?> c : tags) {
			Assertions.assertThat(ctx.getBean(c)).isNotNull();
		}
	}

	@Test
	public void testConnectivity() throws Exception {
		Assume.assumeTrue(configExists());

		ApplicationContext ctx = new SpringApplicationBuilder()
				.sources(MyConfig.class)
				.properties(defaultConfig())
				.run();

		CloudFoundryOperations operations = ctx.getBean(CloudFoundryOperations.class);
		Assert.assertNotNull(operations);
		OrganizationSummary summary = operations
				.organizations()
				.list()
				.blockFirst();
		Assertions.assertThat(summary).isNotNull();
		Assertions.assertThat(summary.getId()).isNotEmpty();
		Assertions.assertThat(summary.getName()).isNotEmpty();
	}

	@Ignore
	@Test
	public void instances() throws Exception {

		Assume.assumeTrue(configExists());

		Log log = LogFactory.getLog(getClass());
		ApplicationContext ctx = new SpringApplicationBuilder()
				.sources(MyConfig.class)
				.properties(defaultConfig())
				.run();

		CloudFoundryOperations cf = ctx.getBean(CloudFoundryOperations.class);
		Flux<Tuple2<ApplicationDetail, InstanceDetail>> mapMany = cf
				.applications()
				.get(GetApplicationRequest.builder().name("lo-test").build())
				.flatMapMany(applicationDetail -> {
					List<InstanceDetail> instanceDetails = applicationDetail.getInstanceDetails();
					Flux<InstanceDetail> ids = Flux.fromStream(instanceDetails.stream());
					Flux<ApplicationDetail> generate = Flux.generate(sink -> sink.next(applicationDetail));
					return generate.zipWith(ids);
				});
		mapMany
				.subscribe(p ->
						log.info(p.getT1().getName() + ':' + p.getT1().getId() + ':' + p.getT2().getIndex() + ':' +
								p.getT2().getState()));

		Thread.sleep(5 * 1000);
	}

}