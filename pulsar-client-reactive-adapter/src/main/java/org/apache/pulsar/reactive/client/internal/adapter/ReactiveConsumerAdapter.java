/*
 * Copyright 2022 the original author or authors.
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

package org.apache.pulsar.reactive.client.internal.adapter;

import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ReactiveConsumerAdapter<T> {

	private final Supplier<PulsarClient> pulsarClientSupplier;

	private final Function<PulsarClient, ConsumerBuilder<T>> consumerBuilderFactory;

	private final Logger LOG = LoggerFactory.getLogger(ReactiveConsumerAdapter.class);

	ReactiveConsumerAdapter(Supplier<PulsarClient> pulsarClientSupplier,
			Function<PulsarClient, ConsumerBuilder<T>> consumerBuilderFactory) {
		this.pulsarClientSupplier = pulsarClientSupplier;
		this.consumerBuilderFactory = consumerBuilderFactory;
	}

	private Mono<Consumer<T>> createConsumerMono() {
		return AdapterImplementationFactory.adaptPulsarFuture(
				() -> this.consumerBuilderFactory.apply(this.pulsarClientSupplier.get()).subscribeAsync());
	}

	private Mono<Void> closeConsumer(Consumer<?> consumer) {
		return Mono.fromFuture(consumer::closeAsync).doOnSuccess((__) -> this.LOG.info("Consumer closed {}", consumer));
	}

	<R> Mono<R> usingConsumer(Function<Consumer<T>, Mono<R>> usingConsumerAction) {
		return Mono.usingWhen(createConsumerMono(), usingConsumerAction, this::closeConsumer);
	}

	<R> Flux<R> usingConsumerMany(Function<Consumer<T>, Flux<R>> usingConsumerAction) {
		return Flux.usingWhen(createConsumerMono(), usingConsumerAction, this::closeConsumer);
	}

}
