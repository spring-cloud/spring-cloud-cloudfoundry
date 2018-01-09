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

package org.springframework.cloud.cloudfoundry.discovery;

import java.util.List;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes a {@link HeartbeatEvent} listing the available services as its state
 * indicator. If consumers detect a change it means there is a change in the catalog.
 *
 * @author Dave Syer
 *
 */
@Component
public class CloudFoundryHeartbeatSender implements ApplicationEventPublisherAware {

	private final CloudFoundryDiscoveryClient client;
	private ApplicationEventPublisher publisher;

	public CloudFoundryHeartbeatSender(CloudFoundryDiscoveryClient client) {
		this.client = client;
	}

	@Scheduled(fixedDelayString = "${spring.cloud.cloudfoundry.discovery.heartbeatFrequency:5000}")
	public void poll() {
		if (this.publisher != null) {
			List<String> services = this.client.getServices();
			this.publisher.publishEvent(new HeartbeatEvent(this.client, services));
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

}
