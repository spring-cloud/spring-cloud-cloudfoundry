/*
 * Copyright 2013-2018 the original author or authors.
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
 * @author Josh Long
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
