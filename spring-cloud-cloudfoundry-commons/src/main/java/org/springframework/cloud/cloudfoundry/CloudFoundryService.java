package org.springframework.cloud.cloudfoundry;


import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.InstanceDetail;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

/**
 * Supports the discovery of a combination of an application instance's URI, port,
 * application ID, and application index.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
public class CloudFoundryService {

	private final CloudFoundryOperations cloudFoundryOperations;

	public CloudFoundryService(CloudFoundryOperations cloudFoundryOperations) {
		this.cloudFoundryOperations = cloudFoundryOperations;
	}

	public Flux<Tuple2<ApplicationDetail, InstanceDetail>> getApplicationInstances(String serviceId) {
		GetApplicationRequest applicationRequest = GetApplicationRequest.builder().name(serviceId).build();
		return this.cloudFoundryOperations
				.applications()
				.get(applicationRequest)
				.flatMapMany(applicationDetail -> {
					Flux<InstanceDetail> ids = Flux.fromStream(applicationDetail.getInstanceDetails().stream())
							.filter(id -> id.getState().equalsIgnoreCase("RUNNING"));
					Flux<ApplicationDetail> generate = Flux.generate(sink -> sink.next(applicationDetail));
					return generate.zipWith(ids);
				});
	}

}
