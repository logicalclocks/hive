variable "VERSION" {
  default = "3.0.0.13.8"
}

variable "REGISTRY" {
  default = "$REGISTRY"
}

variable "REGISTRY_PROJECT" {
  default = "$REGISTRY_PROJECT"
}

variable "COMMIT_HASH" {
  default = "latest"
}

variable "JIRA_TAG" {
  default = "NOJIRA"
}

target "hive" {
    dockerfile = "./Dockerfile"
    platforms = ["linux/amd64"]
    output = ["type=registry"]
    attest = ["type=sbom"]
    tags = [
        "${REGISTRY}/${REGISTRY_PROJECT}/hive:${VERSION}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hive:${VERSION}-${COMMIT_HASH}",
        "${REGISTRY}/${REGISTRY_PROJECT}/hive:${JIRA_TAG}",
    ]
    cache-from= ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hive:cache-${JIRA_TAG}"]
    cache-to = ["type=registry,ref=${REGISTRY}/${REGISTRY_PROJECT}/hive:cache-${JIRA_TAG},mode=max,image-manifest=true"]
    secret = ["id=wgetrc,src=./.wgetrc"]
}

