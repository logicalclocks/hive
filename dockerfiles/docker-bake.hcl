variable "HIVE_VERSION" {
  default = "$HIVE_VERSION"
}

variable "NEXUS_HIVE_URL" {
  default = "$NEXUS_HIVE_URL"
}

variable "HIVE_TAR_NAME" {
  default = "$HIVE_TAR_NAME"
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


target "hive" {
    dockerfile = "./Dockerfile"
    platforms = ["linux/amd64"]
    output = ["type=registry"]
    attest = ["type=sbom"]
    args = {
        HIVE_VERSION = "${HIVE_VERSION}",
        NEXUS_HIVE_URL = "${NEXUS_HIVE_URL}",
        HIVE_TAR_NAME = "${HIVE_TAR_NAME}",
    }
    tags = [
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}-${COMMIT_HASH}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:${HIVE_VERSION}-${JIRA_TAG}",
    ]
    cache-from= ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:cache-${JIRA_TAG}"]
    cache-to = ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hopsworks/hive:cache-${JIRA_TAG},mode=max,image-manifest=true"]
    secret = ["id=wgetrc,src=./.wgetrc"]
}

