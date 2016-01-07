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

package org.cloudfoundry.client;

import org.cloudfoundry.AbstractIntegrationTest;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.ApplicationResource;
import org.cloudfoundry.client.v2.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v2.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationsResponse;
import org.cloudfoundry.operations.v2.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.fn.Function;
import reactor.fn.tuple.Tuple2;
import reactor.rx.Stream;
import reactor.rx.Streams;

import static org.junit.Assert.assertEquals;

public final class ApplicationsTest extends AbstractIntegrationTest {

//    @Before
//    public void before() throws Exception {
//    }
//
//    @After
//    public void after() {
//    }

    @Test
    public void create() {
        this.countApplications().subscribe(testSubscriber()
                .assertEquals(0L));

//        this.spaceId
//                .flatMap(spaceId -> {
//                    CreateApplicationRequest request = CreateApplicationRequest.builder()
//                            .name(this.applicationName)
//                            .spaceId(spaceId)
//                            .build();
//
//                    Stream<ApplicationEntity> entity = Streams
//                            .wrap(this.cloudFoundryClient.applicationsV2().create(request))
//                            .map(Resources::getEntity);
//
//                    return Streams.zip(this.spaceId, entity);
//                })
//                .subscribe(this.<Tuple2<String, ApplicationEntity>>testSubscriber()
//                        .assertThat(this::assertApplication));
    }

    @Test
    public void list() {
        this.createApplication("test-application")
                .flatMap((applicationId) -> {
                    this.countApplications().subscribe(testSubscriber().assertEquals(1L));
                    return Streams.just(applicationId);
                })
                .flatMap((applicationId) -> {
                    return this.deleteApplication(Streams.just(applicationId));
                })
                .flatMap((foo) -> {
                    this.countApplications().subscribe(testSubscriber().assertEquals(0L));
                    return null;
                });
    }

//    @Test
//    public void delete() {
//
//    }

    // Utility methods

    public Stream<Long> countApplications() {
        ListApplicationsRequest request = ListApplicationsRequest.builder()
                .build();

        return Streams.wrap(this.cloudFoundryClient.applicationsV2().list(request))
                .flatMap(Resources::getResources)
                .count();
    }

    private Stream<Void> deleteApplication(Stream<String> applicationIds) {
        return applicationIds.flatMap((applicationId) -> {
                    DeleteApplicationRequest request = DeleteApplicationRequest.builder()
                            .id(applicationId)
                            .build();

                    return Streams.wrap(this.cloudFoundryClient.applicationsV2().delete(request));
                });
    }

    private Stream<String> createApplication(String name) {
        return this.spaceId
                .flatMap(spaceId -> {
                    CreateApplicationRequest request = CreateApplicationRequest.builder()
                            .name(name)
                            .spaceId(spaceId)
                            .build();

                    return this.cloudFoundryClient.applicationsV2().create(request);

                })
                .map(response -> response.getMetadata().getId())
                .take(1)
                .cache(1);
    }

//    private void assertApplication(Tuple2<String, ApplicationEntity> tuple) {
//        String spaceId = tuple.t1;
//        ApplicationEntity entity = tuple.t2;
//
//        assertEquals(spaceId, entity.getSpaceId());
//    }

}
