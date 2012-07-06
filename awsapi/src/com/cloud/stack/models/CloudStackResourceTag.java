/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
package com.cloud.stack.models;

import com.google.gson.annotations.SerializedName;

public class CloudStackResourceTag {
    @SerializedName(ApiConstants.RESOURCE_ID)
    private String resourceId;
    @SerializedName(ApiConstants.RESOURCE_TYPE)
    private String resourceType;
    @SerializedName(ApiConstants.KEY)
    private String key;
    @SerializedName(ApiConstants.VALUE)
    private String value;

    public CloudStackResourceTag() {
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
