/*
* Copyright (c) 2010 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*           http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ovirt.engine.api.resource;


public class ApiMediaType extends javax.ws.rs.core.MediaType {
    public final static String APPLICATION_X_YAML = "application/x-yaml";
    public final static javax.ws.rs.core.MediaType APPLICATION_X_YAML_TYPE =
        new javax.ws.rs.core.MediaType("application", "x-yaml");
    public final static String APPLICATION_PDF = "application/pdf";
    public final static String APPLICATION_X_VIRT_VIEWER = "application/x-virt-viewer";
}