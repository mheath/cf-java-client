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
import org.cloudfoundry.client.v2.spaces.CreateSpaceRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.operations.v2.Resources;
import org.junit.Test;
import reactor.fn.tuple.Tuple3;
import reactor.rx.Stream;
import reactor.rx.Streams;

import static org.junit.Assert.assertEquals;

public final class SpacesTest extends AbstractIntegrationTest {

    @Test
    public void get() {
        Streams
                .wrap(this.organizationId)
                .flatMap(orgId -> {
                    CreateSpaceRequest request = CreateSpaceRequest.builder()
                            .organizationId(orgId)
                            .name("test-space-name")
                            .build();

                    return Streams.wrap(this.cloudFoundryClient.spaces().create(request))
                            .map(Resources::getId);
                })
                .flatMap(spaceId -> {
                    GetSpaceRequest request = GetSpaceRequest.builder()
                            .id(spaceId)
                            .build();

                    Stream<SpaceEntity> entity = Streams
                            .wrap(this.cloudFoundryClient.spaces().get(request))
                            .map(Resources::getEntity);

                    return Streams.zip(this.organizationId, Streams.just("test-space-name"), entity);
                })
                .subscribe(this.<Tuple3<String, String, SpaceEntity>>testSubscriber()
                        .assertThat(this::assertOrganizationIdAndName));

    }
    private void assertOrganizationIdAndName(Tuple3<String, String, SpaceEntity> tuple) {
        String orgId = tuple.t1;
        String name = tuple.t2;
        SpaceEntity entity = tuple.t3;

        assertEquals(orgId, entity.getOrganizationId());
        assertEquals(name, entity.getName());
    }

}
