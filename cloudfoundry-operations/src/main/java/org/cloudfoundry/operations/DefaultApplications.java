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
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.ApplicationResource;
import org.cloudfoundry.client.v2.applications.ApplicationStatisticsRequest;
import org.cloudfoundry.client.v2.applications.ApplicationStatisticsResponse;
import org.cloudfoundry.client.v2.spaces.GetSpaceSummaryRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceSummaryResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceApplicationsRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceApplicationsResponse;
import org.cloudfoundry.client.v2.spaces.SpaceApplicationSummary;
import org.cloudfoundry.operations.v2.Paginated;
import org.reactivestreams.Publisher;
import reactor.Flux;
import reactor.Mono;
import reactor.fn.BiFunction;
import reactor.fn.Function;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;

final class DefaultApplications implements Applications {

    private final CloudFoundryClient cloudFoundryClient;

    private final Mono<String> spaceId;

    DefaultApplications(CloudFoundryClient cloudFoundryClient, Mono<String> spaceId) {
        this.cloudFoundryClient = cloudFoundryClient;
        this.spaceId = spaceId;
    }

    @Override
    public Mono<Application> get(GetApplicationRequest request) {
        return Validators.validate(request)
                .and(this.spaceId)
                .flatMap(new Function<Tuple2<GetApplicationRequest, String>, Publisher<ApplicationResource>>() {
                    @Override
                    public Publisher<ApplicationResource> apply(final Tuple2<GetApplicationRequest, String> tuple) {
                        return Paginated.requestResources(new Function<Integer, Mono<ListSpaceApplicationsResponse>>() {
                            @Override
                            public Mono<ListSpaceApplicationsResponse> apply(Integer page) {
                                return cloudFoundryClient.spaces().listApplications(
                                        ListSpaceApplicationsRequest.builder()
                                                .id(tuple.getT2())
                                                .name(tuple.getT1().getName())
                                                .page(page)
                                                .build()
                                );
                            }
                        });
                    }
                })
                .flatMap(new Function<ApplicationResource, Publisher<Tuple2<ApplicationResource, ApplicationStatisticsResponse>>>() {

                    @Override
                    public Publisher<Tuple2<ApplicationResource, ApplicationStatisticsResponse>> apply(ApplicationResource applicationResource) {
                        Mono<ApplicationStatisticsResponse> appStatsResponse = cloudFoundryClient.applicationsV2().statistics(ApplicationStatisticsRequest.builder()
                                .id(applicationResource.getMetadata().getId())
                                .build());
                        return Flux.just(applicationResource).zipWith(appStatsResponse);
                    }
                })
                .map(new Function<Tuple2<ApplicationResource, ApplicationStatisticsResponse>, Application>() {
                    @Override
                    public Application apply(Tuple2<ApplicationResource, ApplicationStatisticsResponse> tuple) {
                        ApplicationEntity entity = tuple.getT1().getEntity();

                        return Application.builder()
                                .id(tuple.getT1().getMetadata().getId())
                                .diskQuota(entity.getDiskQuota())
                                .memoryLimit(entity.getMemory())
                                .requestedState(entity.getState())
                                .instances(entity.getInstances())
                                .urls(tuple.getT2().get("0").getStatistics().getUris())
                                // grab uris from stats call and use it to populate urls - TODO: how to plumb in stats call?
                                // TODO: set all the other fields - drive by testing
                                .build();
                    }
                }).next();
    }

    @Override
    public Publisher<Application> list() {
        return this.spaceId
                .then(requestSpaceSummary(this.cloudFoundryClient))
                .flatMap(extractApplications())
                .map(toApplication());
    }

    private static Function<GetSpaceSummaryResponse, Publisher<SpaceApplicationSummary>> extractApplications() {
        return new Function<GetSpaceSummaryResponse, Publisher<SpaceApplicationSummary>>() {

            @Override
            public Publisher<SpaceApplicationSummary> apply(GetSpaceSummaryResponse getSpaceSummaryResponse) {
                return Flux.fromIterable(getSpaceSummaryResponse.getApplications());
            }

        };
    }

    private static Function<String, Mono<GetSpaceSummaryResponse>> requestSpaceSummary(final CloudFoundryClient cloudFoundryClient) {
        return new Function<String, Mono<GetSpaceSummaryResponse>>() {

            @Override
            public Mono<GetSpaceSummaryResponse> apply(String targetedSpace) {
                GetSpaceSummaryRequest request = GetSpaceSummaryRequest.builder()
                        .id(targetedSpace)
                        .build();

                return cloudFoundryClient.spaces().getSummary(request);
            }

        };
    }

    private static Function<SpaceApplicationSummary, Application> toApplication() {
        return new Function<SpaceApplicationSummary, Application>() {

            @Override
            public Application apply(SpaceApplicationSummary applicationSummary) {
                return Application.builder()
                        .diskQuota(applicationSummary.getDiskQuota())
                        .id(applicationSummary.getId())
                        .instances(applicationSummary.getInstances())
                        .memoryLimit(applicationSummary.getMemory())
                        .name(applicationSummary.getName())
                        .requestedState(applicationSummary.getState())
                        .runningInstances(applicationSummary.getRunningInstances())
                        .urls(applicationSummary.getUrls())
                        .build();
            }

        };
    }

}
