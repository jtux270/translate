/*
* Copyright (c) 2014 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.core.common.businessentities.network.ExternalSubnet;

public class IpVersionMapper {
    public static IpVersion map(ExternalSubnet.IpVersion entity) {
        switch (entity) {
            case IPV4:
                return IpVersion.V4;
            case IPV6:
                return IpVersion.V6;
            default:
                throw new IllegalArgumentException("Unknown IP version \"" + entity + "\"");
        }
    }

    public static ExternalSubnet.IpVersion map(IpVersion model) {
        switch (model) {
            case V4:
                return ExternalSubnet.IpVersion.IPV4;
            case V6:
                return ExternalSubnet.IpVersion.IPV6;
            default:
                throw new IllegalArgumentException("Unknown IP version \"" + model + "\"");
        }
    }
}
