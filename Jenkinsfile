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

@Library("jenkins-library@main")

import com.logicalclocks.jenkins.k8s.ImageBuilder

pipeline {
  agent { label 'local' }

  options {
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    booleanParam(name: 'BUILD_IMAGE_ONLY', defaultValue: false, description: 'Only build and push the Docker image using an existing Hive package.')
    string(name: 'BRANCH_TO_BUILD', defaultValue: 'branch-4.1', description: 'Git branch to build.')
    string(name: 'MAVEN_CMD', defaultValue: 'mvn', description: 'Maven executable to use.')
    string(name: 'MAVEN_ARGS', defaultValue: 'clean install deploy -Pdist -DskipTests -Denforcer.skip=true', description: 'Maven goals and arguments.')
    string(name: 'MAVEN_DEPLOY_ARGS', defaultValue: 'deploy -Pdist -DskipTests -Denforcer.skip=true', description: 'Maven goals for the second publish to hops-artifacts. Must not include clean or the artifacts built by MAVEN_ARGS are discarded.')
    booleanParam(name: 'PUBLISH_TO_HOPS_ARTIFACTS', defaultValue: true, description: 'Publish the artifacts to hops-artifacts in addition to hive-artifacts, so consumers can resolve io.hops.hive:* with the HopsEE credentials they already have.')
    booleanParam(name: 'FORCE_UPDATE', defaultValue: true, description: 'Pass -U to Maven to refresh snapshot and cached dependency resolution.')
  }

  environment {
    DOCKER_IMAGE = 'maven:3.9.9-eclipse-temurin-17'
    HIVE_PACKAGE_DIR = '/opt/repository/master/hive'
    HOST_MAVEN_REPO = '/home/jenkinsmaster/.m2'
    MAVEN_LOCAL_REPO = '/maven-repo/repository'
    MAVEN_OPTS = '-Xmx4G'
    MAVEN_SETTINGS = "${WORKSPACE}@tmp/mvn-settings.xml"
    HOPS_ARTIFACTS_URL = 'https://nexus.hops.works/repository/hops-artifacts'
    // Nexus proxy of Maven Central, so repo.maven.apache.org stops rate-limiting (HTTP 429)
    // the agent's IP. This softens the symptom only: the 429s are earned by the volume of
    // doomed -tests.jar lookups shade makes, 2029 of them in build #32. See shadeTestJar in
    // druid-handler/pom.xml for the cause.
    CENTRAL_MIRROR_URL = 'https://nexus.hops.works/repository/cache-maven-public/'
    // Be patient with 429/503 responses instead of failing the reactor after 3 tries.
    MAVEN_RETRY_ARGS = '-Daether.connector.http.retryHandler.count=10 -Daether.connector.http.retryHandler.interval=15000'
  }

  stages {
    stage('Checkout') {
      steps {
        deleteDir()
        // Repo URL and credentials come from the job's SCM configuration; only the
        // branch is overridden by the BRANCH_TO_BUILD parameter.
        checkout([$class: 'GitSCM',
          branches: [[name: "${params.BRANCH_TO_BUILD}"]],
          userRemoteConfigs: scm.userRemoteConfigs
        ])
      }
    }

    stage('Prepare Maven') {
      when {
        expression { !params.BUILD_IMAGE_ONLY }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'a0770738-4ef3-4acc-a6ba-097ee6c85b44', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
          sh '''#!/bin/bash -eu
            rm -rf "$WORKSPACE/.m2" "$HOST_MAVEN_REPO/repository/io/hops/hive"
            mkdir -p "$(dirname "$MAVEN_SETTINGS")" "$HOST_MAVEN_REPO/repository"

            # Only mirror central when a proxy URL is configured; an empty CENTRAL_MIRROR_URL
            # must not emit a <mirror> with a blank <url>, which would break all resolution.
            CENTRAL_MIRROR_XML=""
            if [ -n "${CENTRAL_MIRROR_URL:-}" ]; then
              CENTRAL_MIRROR_XML="<mirror>
      <id>hops-central</id>
      <name>Nexus proxy of Maven Central</name>
      <url>${CENTRAL_MIRROR_URL}</url>
      <mirrorOf>central</mirrorOf>
    </mirror>"
            fi

            cat > "$MAVEN_SETTINGS" <<EOF
<settings>
  <localRepository>${MAVEN_LOCAL_REPO}</localRepository>
  <servers>
    <server>
      <id>HopsEE</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
    <server>
      <id>Hops</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
    <server>
      <id>HopsHive</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
    <server>
      <id>hops-releases</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
    <server>
      <id>hive-releases</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
    <server>
      <id>hops-central</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
    <server>
      <id>Hive</id>
      <username>$USERNAME</username>
      <password>$PASSWORD</password>
    </server>
  </servers>
  <mirrors>
    <!--
      Druid's poms declare repository.jboss.org over plain http, and Maven 3.8.1+
      blocks every http:// repository through its built-in maven-default-http-blocker
      mirror. org.hyperic:sigar:1.6.5.132 exists only in that JBoss repo (Central
      returns 404 for it), so with JBoss blocked hive-druid-handler cannot resolve it.
      Re-point the same repository id at its https URL.
    -->
    <mirror>
      <id>jboss-public-https</id>
      <name>JBoss public over https</name>
      <url>https://repository.jboss.org/nexus/content/groups/public/</url>
      <mirrorOf>repository.jboss.org</mirrorOf>
    </mirror>
    ${CENTRAL_MIRROR_XML}
  </mirrors>
</settings>
EOF
          '''
        }
      }
    }

    stage('Resolve Version') {
      steps {
        sh '''#!/bin/bash -eu
          perl -0ne 'if (m{<artifactId>hive</artifactId>\\s*<version>([^<]+)</version>}) { print "$1"; exit }' pom.xml > version.log
          echo "POM_VERSION=$(cat version.log)"
        '''
      }
    }

    stage('Build and Deploy') {
      when {
        expression { !params.BUILD_IMAGE_ONLY }
      }
      steps {
        sh '''#!/bin/bash -eu
          UPDATE_ARG=""
          if [ "$FORCE_UPDATE" = "true" ]; then
            UPDATE_ARG="-U"
          fi

          # Defaults are repeated here because this script runs under -u: Jenkins only exports a
          # parameter as an environment variable once the job has registered it, which happens at
          # the end of the first build after this file adds it. A build triggered with an explicit
          # parameter set that omits these would hit the same gap.
          PUBLISH_HOPS="${PUBLISH_TO_HOPS_ARTIFACTS:-true}"
          DEPLOY_ARGS="${MAVEN_DEPLOY_ARGS:-deploy -Pdist -DskipTests -Denforcer.skip=true}"

          # Both the root pom and standalone-metastore/pom.xml pin distributionManagement to
          # the Hive repo (hive-artifacts). altDeploymentRepository overrides that for every
          # module in the reactor, so a second deploy publishes the same artifacts to
          # hops-artifacts without touching either pom. maven-deploy-plugin is 2.8.2 (inherited
          # from org.apache:apache:23), which requires the three-part id::layout::url form.
          ALT_DEPLOY_REPO=""
          if [ "$PUBLISH_HOPS" = "true" ]; then
            ALT_DEPLOY_REPO="HopsEE::default::${HOPS_ARTIFACTS_URL}"
          fi

          docker run --rm \
            -u "$(id -u):$(id -g)" \
            -v "$WORKSPACE:$WORKSPACE" \
            -v "$(dirname "$MAVEN_SETTINGS"):$(dirname "$MAVEN_SETTINGS")" \
            -v "$HOST_MAVEN_REPO:/maven-repo" \
            -w "$WORKSPACE" \
            -e GITHUB_ACTIONS=true \
            -e HOME=/tmp \
            -e MAVEN_CONFIG=/tmp/maven-config \
            -e MAVEN_LOCAL_REPO="$MAVEN_LOCAL_REPO" \
            -e MAVEN_OPTS="$MAVEN_OPTS" \
            -e MAVEN_CMD="$MAVEN_CMD" \
            -e MAVEN_SETTINGS="$MAVEN_SETTINGS" \
            -e MAVEN_ARGS="$MAVEN_ARGS" \
            -e MAVEN_DEPLOY_ARGS="$DEPLOY_ARGS" \
            -e ALT_DEPLOY_REPO="$ALT_DEPLOY_REPO" \
            -e MAVEN_RETRY_ARGS="$MAVEN_RETRY_ARGS" \
            -e UPDATE_ARG="$UPDATE_ARG" \
            "$DOCKER_IMAGE" \
            bash -lc '
              set -eu
              export PATH="$JAVA_HOME/bin:$PATH"
              JAVA_VERSION="$("$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1 | awk -F"= " "/java.specification.version =/{print \\$2; exit}")"
              if [ "$JAVA_VERSION" != "17" ]; then
                echo "Java 17 is required, but JAVA_HOME=$JAVA_HOME reports java.specification.version=$JAVA_VERSION" >&2
                exit 1
              fi
              test -x "$JAVA_HOME/bin/javadoc"
              "$MAVEN_CMD" -s "$MAVEN_SETTINGS" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" $MAVEN_RETRY_ARGS $UPDATE_ARG $MAVEN_ARGS

              if [ -n "$ALT_DEPLOY_REPO" ]; then
                echo "Publishing the same artifacts to $ALT_DEPLOY_REPO"
                "$MAVEN_CMD" -s "$MAVEN_SETTINGS" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" $MAVEN_RETRY_ARGS \
                  $MAVEN_DEPLOY_ARGS -DaltDeploymentRepository="$ALT_DEPLOY_REPO"
              fi
            '
        '''
      }
    }

    stage('Copy Hive Package') {
      when {
        expression { !params.BUILD_IMAGE_ONLY }
      }
      steps {
        sh '''#!/bin/bash -eu
          HIVE_VERSION="$(tr -d '\\r\\n' < version.log)"
          mkdir -p "$HIVE_PACKAGE_DIR"

          docker run --rm \
            -u "$(id -u):$(id -g)" \
            -v "$WORKSPACE:$WORKSPACE" \
            -v "$(dirname "$MAVEN_SETTINGS"):$(dirname "$MAVEN_SETTINGS")" \
            -v "$HOST_MAVEN_REPO:/maven-repo" \
            -v "$HIVE_PACKAGE_DIR:$HIVE_PACKAGE_DIR" \
            -w "$WORKSPACE" \
            -e GITHUB_ACTIONS=true \
            -e HOME=/tmp \
            -e MAVEN_CONFIG=/tmp/maven-config \
            -e MAVEN_LOCAL_REPO="$MAVEN_LOCAL_REPO" \
            -e MAVEN_OPTS="$MAVEN_OPTS" \
            -e MAVEN_CMD="$MAVEN_CMD" \
            -e MAVEN_SETTINGS="$MAVEN_SETTINGS" \
            -e HIVE_PACKAGE_DIR="$HIVE_PACKAGE_DIR" \
            -e HIVE_VERSION="$HIVE_VERSION" \
            "$DOCKER_IMAGE" \
            bash -lc '
              set -eu
              "$MAVEN_CMD" -s "$MAVEN_SETTINGS" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" dependency:copy \
                -Dartifact=io.hops.hive:hive-packaging:${HIVE_VERSION}:tar.gz:bin \
                -DoutputDirectory="$HIVE_PACKAGE_DIR" \
                -Dmdep.stripVersion=true \
                -U
              mv "$HIVE_PACKAGE_DIR/hive-packaging-bin.tar.gz" "$HIVE_PACKAGE_DIR/hive-packaging-${HIVE_VERSION}-bin.tar.gz"
            '

          ls -l "$HIVE_PACKAGE_DIR/hive-packaging-${HIVE_VERSION}-bin.tar.gz"
        '''
      }
    }

    stage('Build and Push Images') {
      steps {
        script {
          def version = readFile("${env.WORKSPACE}/version.log").trim()
          def imageBuildVersion = readFile("${env.WORKSPACE}/dockerfiles/image-build-version.properties")
              .readLines()
              .find { line -> line.trim() && !line.trim().startsWith('#') && line.contains('IMAGE_BUILD_VERSION=') }
              ?.split('=', 2)[1]
              ?.trim()

          if (!imageBuildVersion) {
            error('IMAGE_BUILD_VERSION is missing from dockerfiles/image-build-version.properties')
          }

          withCredentials([usernamePassword(credentialsId: 'a0770738-4ef3-4acc-a6ba-097ee6c85b44', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            withEnv(["HIVE_VERSION=${version}", "IMAGE_BUILD_VERSION=${imageBuildVersion}"]) {
              def builder = new ImageBuilder(this)

              sh '''#!/bin/bash -eu
                test -f "$HIVE_PACKAGE_DIR/hive-packaging-${HIVE_VERSION}-bin.tar.gz"
                cp "$HIVE_PACKAGE_DIR/hive-packaging-${HIVE_VERSION}-bin.tar.gz" "$WORKSPACE/dockerfiles/hive-packaging-${HIVE_VERSION}-bin.tar.gz"
                printf "user=%s\npassword=%s" "$USERNAME" "$PASSWORD" > "$WORKSPACE/dockerfiles/wgetrc"
                ls -l "$WORKSPACE/dockerfiles"
              '''

              def manifest = readFile("${env.WORKSPACE}/dockerfiles/build-manifest.json")
              builder.run(manifest)
            }
          }
        }
      }
    }
  }

  post {
    always {
      sh '''#!/bin/bash
        rm -f "$MAVEN_SETTINGS"
      '''
      archiveArtifacts artifacts: 'version.log', allowEmptyArchive: true
    }
  }
}
