package org.springframework.cloud.cloudfoundry;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.routing.ReactorRoutingClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.routing.RoutingClient;
import org.cloudfoundry.uaa.UaaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Provides auto-configuration for the Reactor-based Cloud Foundry client v3.x.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 * @author Ben Hale
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.cloud.cloudfoundry", name = {"username", "password", "org", "space"})
@ConditionalOnClass(name = {"reactor.core.publisher.Flux", "org.cloudfoundry.operations.DefaultCloudFoundryOperations",
		"org.cloudfoundry.reactor.client.ReactorCloudFoundryClient", "org.reactivestreams.Publisher"})
@EnableConfigurationProperties(CloudFoundryProperties.class)
public class CloudFoundryClientAutoConfiguration {

	private final CloudFoundryProperties cloudFoundryProperties;

	public CloudFoundryClientAutoConfiguration(CloudFoundryProperties cfp) {
		this.cloudFoundryProperties = cfp;
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	CloudFoundryService cloudFoundryService(CloudFoundryOperations cloudFoundryOperations) {
		return new CloudFoundryService(cloudFoundryOperations);
	}

	@Bean
	@Lazy
	ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@Lazy
	DefaultCloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient,
	                                                     DopplerClient dopplerClient,
	                                                     RoutingClient routingClient,
	                                                     UaaClient uaaClient) {
		String organization = this.cloudFoundryProperties.getOrg();
		String space = this.cloudFoundryProperties.getSpace();
		return DefaultCloudFoundryOperations
				.builder()
				.cloudFoundryClient(cloudFoundryClient)
				.dopplerClient(dopplerClient)
				.routingClient(routingClient)
				.uaaClient(uaaClient)
				.organization(organization)
				.space(space)
				.build();
	}

	@Bean
	@Lazy
	DefaultConnectionContext connectionContext() {

		String apiHost = this.cloudFoundryProperties.getUrl();
		Boolean skipSslValidation = this.cloudFoundryProperties.isSkipSslValidation();

		return DefaultConnectionContext.builder()
				.apiHost(apiHost)
				.skipSslValidation(skipSslValidation)
				.build();
	}

	@Bean
	@Lazy
	DopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@Lazy
	RoutingClient routingClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorRoutingClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@Lazy
	PasswordGrantTokenProvider tokenProvider() {
		String username = this.cloudFoundryProperties.getUsername();
		String password = this.cloudFoundryProperties.getPassword();
		return PasswordGrantTokenProvider.builder()
				.password(password)
				.username(username)
				.build();
	}

	@Bean
	@Lazy
	ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}
}


