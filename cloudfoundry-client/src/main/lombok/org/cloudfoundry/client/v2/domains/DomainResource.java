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

package org.cloudfoundry.client.v2.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudfoundry.client.v2.Resource;

/**
 * The response payload for the Domain resource
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class DomainResource extends Resource<DomainEntity> {

    @Builder
    DomainResource(@JsonProperty("entity") DomainEntity entity,
                   @JsonProperty("metadata") Metadata metadata) {
        super(entity, metadata);
    }

}