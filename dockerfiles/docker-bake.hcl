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


target "hive" {
    dockerfile = "./Dockerfile"
    platforms = ["linux/amd64"]
    output = ["type=registry"]
    attest = ["type=sbom"]
    args = {
        HIVE_VERSION = "${HIVE_VERSION}",
    }
    tags = [
        "${REGISTRY}/${REGISTRY_PROJECT}/hive:${HIVE_VERSION}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hive:${HIVE_VERSION}-${COMMIT_HASH}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hive:${JIRA_TAG}",
    ]
    cache-from= ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hive:cache-${JIRA_TAG}"]
    cache-to = ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hive:cache-${JIRA_TAG},mode=max,image-manifest=true"]
    secret = ["id=wgetrc,src=./.wgetrc"]
}

