/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
variable "HIVE_VERSION" {
  default = "$HIVE_VERSION"
}


variable "REGISTRY" {
  default = "$REGISTRY"
}

variable "REGISTRY_PROJECT" {
  default = "$REGISTRY_PROJECT"
}

variable "COMMIT_HASH" {
  default = "$COMMIT_HASH"
}

variable "JIRA_TAG" {
  default = "$JIRA_TAG"
}

variable "IMAGE_BUILD_VERSION" {
  default = "$IMAGE_BUILD_VERSION"
}


target "hive" {
    dockerfile = "./Dockerfile"
    platforms = ["linux/amd64"]
    output = ["type=registry"]
    attest = ["type=sbom"]
    args = {
        HIVE_VERSION = "${HIVE_VERSION}",
    }
    tags = [
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${trimsuffix(HIVE_VERSION, "-SNAPSHOT")}-${IMAGE_BUILD_VERSION}${HIVE_VERSION != trimsuffix(HIVE_VERSION, "-SNAPSHOT") ? "-SNAPSHOT" : ""}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}-${IMAGE_BUILD_VERSION}-${COMMIT_HASH}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}-${IMAGE_BUILD_VERSION}-${JIRA_TAG}",
    ]
    cache-from= ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:cache-${JIRA_TAG}"]
    cache-to = ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:cache-${JIRA_TAG},mode=max,image-manifest=true"]
    secret = ["id=wgetrc,src=./.wgetrc"]
}

