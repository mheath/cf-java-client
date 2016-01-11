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

import org.reactivestreams.Publisher;
import reactor.Mono;

/**
 * Main entry point to the Cloud Foundry Applications Operations API
 */
public interface Applications {

    /**
     * Lists the applications
     *
     * @return the applications
     */
    Publisher<Application> list();

    /**
     * Gets information for a specific application
     * 
     * @param request the get application request
     * @return the application
     */
    Mono<Application> get(GetApplicationRequest request);

}
