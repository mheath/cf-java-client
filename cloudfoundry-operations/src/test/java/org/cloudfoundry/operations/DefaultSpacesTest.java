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

package org.cloudfoundry.operations;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationResource;
import org.cloudfoundry.client.v2.domains.DomainResource;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesResponse;
import org.cloudfoundry.client.v2.securitygroups.SecurityGroupResource;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.cloudfoundry.client.v2.spacequotadefinitions.GetSpaceQuotaDefinitionRequest;
import org.cloudfoundry.client.v2.spacequotadefinitions.GetSpaceQuotaDefinitionResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceApplicationsRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceApplicationsResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceDomainsRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceDomainsResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceSecurityGroupsRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceSecurityGroupsResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceServicesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceServicesResponse;
import org.cloudfoundry.client.v2.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpacesResponse;
import org.cloudfoundry.client.v2.spaces.SpaceResource;
import org.cloudfoundry.utils.test.TestSubscriber;
import org.junit.Before;
import org.reactivestreams.Publisher;
import reactor.Mono;

import static org.cloudfoundry.operations.v2.TestObjects.fill;
import static org.mockito.Mockito.when;

public final class DefaultSpacesTest {

    private static void setupExpectations(CloudFoundryClient cloudFoundryClient) {
        ListOrganizationSpacesRequest request1 = fill(ListOrganizationSpacesRequest.builder())
                .name("test-space-name")
                .id(null)
                .organizationId("test-organizationId")
                .build();
        ListOrganizationSpacesResponse response1 = fill(ListOrganizationSpacesResponse.builder())
                .resource(fill(SpaceResource.builder(), "space-").build())
                .build();
        when(cloudFoundryClient.organizations().listSpaces(request1)).thenReturn(Mono.just(response1));

        ListSpaceApplicationsRequest request2 = fill(ListSpaceApplicationsRequest.builder(), "spaceApplications-")
                .id("test-space-id")
                .diego(null)
                .build();
        ListSpaceApplicationsResponse response2 = fill(ListSpaceApplicationsResponse.builder())
                .resource(fill(ApplicationResource.builder(), "spaceApplication-").build())
                .build();
        when(cloudFoundryClient.spaces().listApplications(request2)).thenReturn(Mono.just(response2));

        ListSpaceDomainsRequest request3 = fill(ListSpaceDomainsRequest.builder())
                .id("test-space-id")
                .build();
        ListSpaceDomainsResponse response3 = fill(ListSpaceDomainsResponse.builder())
                .resource(fill(DomainResource.builder(), "spaceDomain-").build())
                .build();
        when(cloudFoundryClient.spaces().listDomains(request3)).thenReturn(Mono.just(response3));

        GetOrganizationRequest request4 = fill(GetOrganizationRequest.builder())
                .id("test-space-organizationId")
                .build();
        GetOrganizationResponse response4 = fill(GetOrganizationResponse.builder(), "organization-").build();
        when(cloudFoundryClient.organizations().get(request4)).thenReturn(Mono.just(response4));

        ListSpaceSecurityGroupsRequest request5 = fill(ListSpaceSecurityGroupsRequest.builder())
                .id("test-space-id")
                .build();
        ListSpaceSecurityGroupsResponse response5 = fill(ListSpaceSecurityGroupsResponse.builder())
                .resource(fill(SecurityGroupResource.builder(), "securityGroup-").build())
                .build();
        when(cloudFoundryClient.spaces().listSecurityGroups(request5)).thenReturn(Mono.just(response5));

        ListSpaceServicesRequest request6 = fill(ListSpaceServicesRequest.builder())
                .id("test-space-id")
                .build();
        ListSpaceServicesResponse response6 = fill(ListSpaceServicesResponse.builder())
                .resource(fill(ServiceResource.builder(), "service-").build())
                .build();
        when(cloudFoundryClient.spaces().listServices(request6)).thenReturn(Mono.just(response6));
    }

    public static final class Get extends AbstractOperationsApiTest<SpaceDetail> {

        private final Spaces spaces = new DefaultSpaces(this.cloudFoundryClient, Mono.just("test-organizationId"));

        @Before
        public void setUp() throws Exception {
            setupExpectations(this.cloudFoundryClient);

            GetSpaceQuotaDefinitionRequest request = fill(GetSpaceQuotaDefinitionRequest.builder())
                    .id("test-space-spaceQuotaDefinitionId")
                    .build();
            GetSpaceQuotaDefinitionResponse response = fill(GetSpaceQuotaDefinitionResponse.builder(), "spaceQuota-")
                    .build();
            when(this.cloudFoundryClient.spaceQuotaDefinitions().get(request)).thenReturn(Mono.just(response));
        }

        @Override
        protected void assertions(TestSubscriber<SpaceDetail> testSubscriber) throws Exception {
            testSubscriber
                    .assertEquals(SpaceDetail.builder()
                            .id("test-space-id")
                            .name("test-space-name")
                            .application("test-spaceApplication-name")
                            .domain("test-spaceDomain-name")
                            .organization("test-organization-name")
                            .securityGroup("test-securityGroup-name")
                            .service("test-service-label")
                            .spaceQuota(Optional.of(fill(SpaceQuota.builder(), "spaceQuota-").build()))
                            .build());
        }

        @Override
        protected Publisher<SpaceDetail> invoke() {
            GetSpaceRequest request = fill(GetSpaceRequest.builder(), "space-").build();

            return this.spaces.get(request);
        }

    }

    public static final class GetNoOrganization extends AbstractOperationsApiTest<SpaceDetail> {

        private final Spaces spaces = new DefaultSpaces(this.cloudFoundryClient, MISSING_ID);

        @Override
        protected void assertions(TestSubscriber<SpaceDetail> testSubscriber) throws Exception {
            testSubscriber
                    .assertError(IllegalStateException.class);
        }

        @Override
        protected Publisher<SpaceDetail> invoke() {
            return this.spaces.get(fill(GetSpaceRequest.builder()).build());
        }

    }

    public static final class GetNoSpaceQuota extends AbstractOperationsApiTest<SpaceDetail> {

        private final Spaces spaces = new DefaultSpaces(this.cloudFoundryClient, Mono.just("test-organizationId"));

        @Before
        public void setUp() throws Exception {
            setupExpectations(this.cloudFoundryClient);
        }

        @Override
        protected void assertions(TestSubscriber<SpaceDetail> testSubscriber) throws Exception {
            testSubscriber
                    .assertEquals(fill(SpaceDetail.builder(), "space-")
                            .application("test-spaceApplication-name")
                            .domain("test-spaceDomain-name")
                            .organization("test-organization-name")
                            .securityGroup("test-securityGroup-name")
                            .service("test-service-label")
                            .spaceQuota(Optional.<SpaceQuota>empty())
                            .build());
        }

        @Override
        protected Publisher<SpaceDetail> invoke() {
            GetSpaceRequest request = GetSpaceRequest.builder()
                    .name("test-space-name")
                    .securityGroupRules(false)
                    .build();

            return this.spaces.get(request);
        }
    }

    public static final class List extends AbstractOperationsApiTest<SpaceSummary> {

        private final Spaces spaces = new DefaultSpaces(this.cloudFoundryClient, Mono.just("test-organizationId"));

        @Before
        public void setUp() throws Exception {
            ListSpacesRequest request1 = ListSpacesRequest.builder()
                    .page(1)
                    .organizationId("test-organizationId")
                    .build();
            ListSpacesResponse response1 = fill(ListSpacesResponse.builder(), "1")
                    .resource(fill(SpaceResource.builder(), "space1-").build())
                    .totalPages(2)
                    .build();
            when(this.cloudFoundryClient.spaces().list(request1)).thenReturn(Mono.just(response1));

            ListSpacesRequest request2 = ListSpacesRequest.builder()
                    .organizationId("test-organizationId")
                    .page(2)
                    .build();
            ListSpacesResponse response2 = fill(ListSpacesResponse.builder())
                    .resource(fill(SpaceResource.builder(), "space2-").build())
                    .totalPages(2)
                    .build();
            when(this.cloudFoundryClient.spaces().list(request2)).thenReturn(Mono.just(response2));
        }

        @Override
        protected void assertions(TestSubscriber<SpaceSummary> testSubscriber) throws Exception {
            testSubscriber
                    .assertEquals(fill(SpaceSummary.builder(), "space1-").build())
                    .assertEquals(fill(SpaceSummary.builder(), "space2-").build())
            ;
        }

        @Override
        protected Publisher<SpaceSummary> invoke() {
            return this.spaces.list();
        }

    }

    public static final class ListNoOrganization extends AbstractOperationsApiTest<SpaceSummary> {

        private final Spaces spaces = new DefaultSpaces(this.cloudFoundryClient, MISSING_ID);

        @Override
        protected void assertions(TestSubscriber<SpaceSummary> testSubscriber) throws Exception {
            testSubscriber
                    .assertError(IllegalStateException.class);
        }

        @Override
        protected Publisher<SpaceSummary> invoke() {
            return this.spaces.list();
        }

    }

}
