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

package org.cloudfoundry.operations.v2;

import org.cloudfoundry.client.v2.PaginatedResponse;
import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;
import reactor.rx.Streams;

import static org.mockito.Mockito.when;

public final class TestPaginated {

    private TestPaginated() {
    }

    public static <REQ, RSP extends PaginatedResponse> void expectPages(Function<Integer, REQ> requestProducer, Function<Integer, RSP> responseProducer,
                                                                        final Function<REQ, Publisher<RSP>> invoker) {
        Streams.just(requestPage(requestProducer, responseProducer).apply(1))
                .flatMap(requestAdditionalPages(requestProducer, responseProducer))
                .consume(new Consumer<Tuple2<REQ, RSP>>() {

                    @Override
                    public void accept(Tuple2<REQ, RSP> page) {
                        when(invoker.apply(page.getT1())).thenReturn(Publishers.just(page.getT2()));
                    }

                });
    }

    private static <REQ, RSP extends PaginatedResponse> Function<Tuple2<REQ, RSP>, Publisher<Tuple2<REQ, RSP>>> requestAdditionalPages(final Function<Integer, REQ> requestProducer,
                                                                                                                                       final Function<Integer, RSP> responseProducer) {
        return new Function<Tuple2<REQ, RSP>, Publisher<Tuple2<REQ, RSP>>>() {

            @Override
            public Publisher<Tuple2<REQ, RSP>> apply(Tuple2<REQ, RSP> page) {
                RSP response = page.getT2();

                Integer totalPages = response.getTotalPages();
                if (totalPages == null) {
                    throw new IllegalStateException(String.format("Page response (class %s) has no total pages set", response.getClass().getCanonicalName()));
                }

                return Streams.range(2, totalPages - 1)
                        .map(requestPage(requestProducer, responseProducer))
                        .startWith(Streams.just(page));
            }

        };

    }

    private static <REQ, RSP extends PaginatedResponse> Function<Integer, Tuple2<REQ, RSP>> requestPage(final Function<Integer, REQ> requestProducer,
                                                                                                        final Function<Integer, RSP> responseProducer) {
        return new Function<Integer, Tuple2<REQ, RSP>>() {

            @Override
            public Tuple2<REQ, RSP> apply(Integer page) {
                return Tuple.of(requestProducer.apply(page), responseProducer.apply(page));
            }

        };
    }

}
