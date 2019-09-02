/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.cloudfoundry.discovery.reactive;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes a {@link HeartbeatEvent} with a `Flux` of services as its state
 * indicator. If consumers detect a change it doesn't necessarily mean there is a change in the catalog.
 *
 * @author Tim Ysewyn
 */
@Component
public class CloudFoundryReactiveHeartbeatSender implements ApplicationEventPublisherAware {

	private final CloudFoundryReactiveDiscoveryClient client;

	private ApplicationEventPublisher publisher;

	public CloudFoundryReactiveHeartbeatSender(CloudFoundryReactiveDiscoveryClient client) {
		this.client = client;
	}

	@Scheduled(
			fixedDelayString = "${spring.cloud.cloudfoundry.discovery.heartbeatFrequency:5000}")
	public void poll() {
		if (this.publisher != null) {
			this.publisher.publishEvent(new HeartbeatEvent(this.client, this.client.getServices()));
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

}
