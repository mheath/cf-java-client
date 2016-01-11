/*
 * Copyright 2013-2016 the original author or authors.
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

package org.cloudfoundry;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v2.domains.DeleteDomainRequest;
import org.cloudfoundry.client.v2.domains.ListDomainsRequest;
import org.cloudfoundry.client.v2.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v2.routes.ListRoutesRequest;
import org.cloudfoundry.client.v2.spaces.DeleteSpaceRequest;
import org.cloudfoundry.client.v2.spaces.ListSpacesRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.v2.Paginated;
import org.cloudfoundry.operations.v2.Resources;
import org.cloudfoundry.utils.test.TestSubscriber;
import org.junit.After;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.Mono;
import reactor.fn.tuple.Tuple2;
import reactor.rx.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IntegrationTestConfiguration.class)
public abstract class AbstractIntegrationTest {

    private final Logger logger = LoggerFactory.getLogger("test");

    private final TestSubscriber<?> testSubscriber = new TestSubscriber<>();

    @Autowired
    protected CloudFoundryClient cloudFoundryClient;

    @Autowired
    protected CloudFoundryOperations cloudFoundryOperations;

    @Autowired
    protected Mono<String> organizationId;

    @Autowired
    protected Mono<String> spaceId;

    @Value("${test.space}")
    protected String spaceName;

    @After
    public final void cleanup() throws Exception {
        cleanupApplications(this.cloudFoundryClient)
                .after(() -> cleanupRoutes(this.cloudFoundryClient))
                .after(() -> cleanupDomains(this.cloudFoundryClient))
                .after(() -> cleanupSpaces(this.spaceId, this.cloudFoundryClient))
                .doOnSubscribe(s -> this.logger.debug(">> CLEANUP <<"))
                .doOnComplete(() -> this.logger.debug("<< CLEANUP >>"))
                .after()
                .get();
    }

    @After
    public final void verify() throws InterruptedException {
        this.testSubscriber.verify(10, SECONDS);
    }

    protected final <T> void assertTupleEquality(Tuple2<T, T> tuple) {
        T actual = tuple.t1;
        T expected = tuple.t2;

        assertEquals(expected, actual);
    }

    @SuppressWarnings("unchecked")
    protected <T> TestSubscriber<T> testSubscriber() {
        return (TestSubscriber<T>) this.testSubscriber;
    }

    private static Stream<Void> cleanupApplications(CloudFoundryClient cloudFoundryClient) {
        return Paginated
                .requestResources(page -> {
                    ListApplicationsRequest request = ListApplicationsRequest.builder()
                            .page(page)
                            .build();

                    return cloudFoundryClient.applicationsV2().list(request);
                })
                .flatMap(response -> {
                    DeleteApplicationRequest request = DeleteApplicationRequest.builder()
                            .id(Resources.getId(response))
                            .build();

                    return cloudFoundryClient.applicationsV2().delete(request);
                });
    }

    private static Stream<Void> cleanupDomains(CloudFoundryClient cloudFoundryClient) {
        return Paginated
                .requestResources(page -> {
                    ListDomainsRequest request = ListDomainsRequest.builder()
                            .page(page)
                            .build();

                    return cloudFoundryClient.domains().list(request);
                })
                .filter(response -> {
                    String name = Resources.getEntity(response).getName();
                    return !name.equals("local.micropcf.io") && !name.endsWith(".xip.io");
                })
                .flatMap(response -> {
                    DeleteDomainRequest request = DeleteDomainRequest.builder()
                            .id(Resources.getId(response))
                            .build();

                    return cloudFoundryClient.domains().delete(request);
                });
    }

    private static Stream<Void> cleanupRoutes(CloudFoundryClient cloudFoundryClient) {
        return Paginated
                .requestResources(page -> {
                    ListRoutesRequest request = ListRoutesRequest.builder()
                            .page(page)
                            .build();

                    return cloudFoundryClient.routes().list(request);
                })
                .flatMap(response -> {
                    DeleteRouteRequest request = DeleteRouteRequest.builder()
                            .id(Resources.getId(response))
                            .build();

                    return cloudFoundryClient.routes().delete(request);
                });
    }

    private static Stream<Void> cleanupSpaces(Mono<String> spaceId, CloudFoundryClient cloudFoundryClient) {
        return Paginated
                .requestResources(page -> {
                    ListSpacesRequest request = ListSpacesRequest.builder()
                            .page(page)
                            .build();

                    return cloudFoundryClient.spaces().list(request);
                })
                .filter(response -> {
                    String id = Resources.getId(response);
                    return !id.equals(spaceId.defaultIfEmpty("").get());
                })
                .flatMap(response -> {
                    DeleteSpaceRequest request = DeleteSpaceRequest.builder()
                            .id(Resources.getId(response))
                            .build();

                    return cloudFoundryClient.spaces().delete(request);
                });
    }

}
