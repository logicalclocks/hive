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
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}-${IMAGE_BUILD_VERSION}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}-${IMAGE_BUILD_VERSION}-${COMMIT_HASH}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}-${IMAGE_BUILD_VERSION}-${JIRA_TAG}",
    ]
    cache-from= ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:cache-${JIRA_TAG}"]
    cache-to = ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:cache-${JIRA_TAG},mode=max,image-manifest=true"]
    secret = ["id=wgetrc,src=./.wgetrc"]
}

