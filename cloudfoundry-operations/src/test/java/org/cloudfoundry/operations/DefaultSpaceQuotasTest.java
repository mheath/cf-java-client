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

package org.cloudfoundry.operations;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Resource.Metadata;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpaceQuotaDefinitionsRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpaceQuotaDefinitionsResponse;
import org.cloudfoundry.client.v2.spacequotadefinitions.SpaceQuotaDefinitionEntity;
import org.cloudfoundry.client.v2.spacequotadefinitions.SpaceQuotaDefinitionResource;
import org.cloudfoundry.operations.v2.TestPaginated;
import org.cloudfoundry.utils.test.TestSubscriber;
import org.junit.Before;
import org.reactivestreams.Publisher;
import reactor.fn.Function;
import reactor.rx.Streams;


public final class DefaultSpaceQuotasTest {

    private static Function<Integer, ListOrganizationSpaceQuotaDefinitionsRequest> getRequest(final String organization) {
        return new Function<Integer, ListOrganizationSpaceQuotaDefinitionsRequest>() {

            @Override
            public ListOrganizationSpaceQuotaDefinitionsRequest apply(Integer page) {
                return ListOrganizationSpaceQuotaDefinitionsRequest.builder()
                        .id(organization)
                        .page(page)
                        .build();
            }

        };
    }

    private static Function<Integer, ListOrganizationSpaceQuotaDefinitionsResponse> getResponse(final int size) {
        return new Function<Integer, ListOrganizationSpaceQuotaDefinitionsResponse>() {

            @Override
            public ListOrganizationSpaceQuotaDefinitionsResponse apply(Integer page) {
                return ListOrganizationSpaceQuotaDefinitionsResponse.builder()
                        .resource(SpaceQuotaDefinitionResource.builder()
                                .metadata(Metadata.builder()
                                        .id("test-id-" + page)
                                        .build())
                                .entity(SpaceQuotaDefinitionEntity.builder()
                                        .instanceMemoryLimit(1024)
                                        .memoryLimit(2048)
                                        .name("test-name-" + page)
                                        .nonBasicServicesAllowed(true)
                                        .organizationId("test-org-id-" + page)
                                        .totalRoutes(10)
                                        .build())
                                .build())
                        .totalPages(size)
                        .build();
            }

        };
    }

    private static SpaceQuota getSpaceQuota(int index) {
        return SpaceQuota.builder()
                .id("test-id-" + index)
                .instanceMemoryLimit(1024)
                .paidServicePlans(true)
                .totalMemoryLimit(2048)
                .totalRoutes(10)
                .name("test-name-" + index)
                .organizationId("test-org-id-" + index)
                .build();
    }

    private static Function<ListOrganizationSpaceQuotaDefinitionsRequest, Publisher<ListOrganizationSpaceQuotaDefinitionsResponse>> makeRequest(final CloudFoundryClient cloudFoundryClient) {
        return new Function<ListOrganizationSpaceQuotaDefinitionsRequest, Publisher<ListOrganizationSpaceQuotaDefinitionsResponse>>() {

            @Override
            public Publisher<ListOrganizationSpaceQuotaDefinitionsResponse> apply(ListOrganizationSpaceQuotaDefinitionsRequest request) {
                return cloudFoundryClient.organizations().listSpaceQuotaDefinitions(request);
            }

        };
    }

    public static final class Get extends AbstractOperationsApiTest<SpaceQuota> {

        private final SpaceQuotas spaceQuotas = new DefaultSpaceQuotas(this.cloudFoundryClient, Streams.just(TEST_ORGANIZATION));

        @Before
        public void setUp() throws Exception {
            TestPaginated.expectPages(getRequest(TEST_ORGANIZATION), getResponse(2), makeRequest(this.cloudFoundryClient));
        }

        @Override
        protected void assertions(TestSubscriber<SpaceQuota> testSubscriber) throws Exception {
            testSubscriber
                    .assertEquals(getSpaceQuota(2));
        }

        @Override
        protected Publisher<SpaceQuota> invoke() {
            GetSpaceQuotaRequest request = GetSpaceQuotaRequest.builder()
                    .name("test-name-2")
                    .build();

            return this.spaceQuotas.get(request);
        }

    }

    public static final class GetInvalid extends AbstractOperationsApiTest<SpaceQuota> {

        private final SpaceQuotas spaceQuotas = new DefaultSpaceQuotas(this.cloudFoundryClient, Streams.just(TEST_ORGANIZATION));

        @Override
        protected void assertions(TestSubscriber<SpaceQuota> testSubscriber) throws Exception {
            testSubscriber
                    .assertError(RequestValidationException.class);
        }

        @Override
        protected Publisher<SpaceQuota> invoke() {
            GetSpaceQuotaRequest request = GetSpaceQuotaRequest.builder()
                    .build();

            return this.spaceQuotas.get(request);
        }
    }

    public static final class GetNoOrganization extends AbstractOperationsApiTest<SpaceQuota> {

        private final SpaceQuotas spaceQuotas = new DefaultSpaceQuotas(this.cloudFoundryClient, MISSING_ID);

        @Override
        protected void assertions(TestSubscriber<SpaceQuota> testSubscriber) throws Exception {
            testSubscriber
                    .assertError(IllegalStateException.class);
        }

        @Override
        protected Publisher<SpaceQuota> invoke() {
            GetSpaceQuotaRequest request = GetSpaceQuotaRequest.builder()
                    .name("test-name-2")
                    .build();

            return this.spaceQuotas.get(request);
        }

    }

    public static final class GetNotFound extends AbstractOperationsApiTest<SpaceQuota> {

        private final SpaceQuotas spaceQuotas = new DefaultSpaceQuotas(this.cloudFoundryClient, Streams.just(TEST_ORGANIZATION));

        @Before
        public void setUp() throws Exception {
            TestPaginated.expectPages(getRequest(TEST_ORGANIZATION), getResponse(2), makeRequest(this.cloudFoundryClient));
        }

        @Override
        protected void assertions(TestSubscriber<SpaceQuota> testSubscriber) throws Exception {
            // expect nothing back
        }

        @Override
        protected Publisher<SpaceQuota> invoke() {
            GetSpaceQuotaRequest request = GetSpaceQuotaRequest.builder()
                    .name("test-name-0")
                    .build();

            return this.spaceQuotas.get(request);
        }

    }

    public static final class List extends AbstractOperationsApiTest<SpaceQuota> {

        private final SpaceQuotas spaceQuotas = new DefaultSpaceQuotas(this.cloudFoundryClient, Streams.just(TEST_ORGANIZATION));

        @Before
        public void setUp() throws Exception {
            TestPaginated.expectPages(getRequest(TEST_ORGANIZATION), getResponse(2), makeRequest(this.cloudFoundryClient));
        }

        @Override
        protected void assertions(TestSubscriber<SpaceQuota> testSubscriber) throws Exception {
            testSubscriber
                    .assertEquals(getSpaceQuota(1))
                    .assertEquals(getSpaceQuota(2));
        }

        @Override
        protected Publisher<SpaceQuota> invoke() {
            return this.spaceQuotas.list();
        }

    }

    public static final class ListNoOrganization extends AbstractOperationsApiTest<SpaceQuota> {

        private final SpaceQuotas spaceQuotas = new DefaultSpaceQuotas(this.cloudFoundryClient, MISSING_ID);

        @Override
        protected void assertions(TestSubscriber<SpaceQuota> testSubscriber) throws Exception {
            testSubscriber
                    .assertError(IllegalStateException.class);
        }

        @Override
        protected Publisher<SpaceQuota> invoke() {
            return this.spaceQuotas.list();
        }

    }

}
