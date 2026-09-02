# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# syntax=docker/dockerfile:experimental
FROM eclipse-temurin:17
ARG HIVE_VERSION=4.1.0
ARG HIVE_TAR_NAME=hive-packaging-${HIVE_VERSION}-bin.tar.gz
ARG HADOOP_VERSION_EE=3.4.3.2-EE-RC3
ARG PROMETHEUS_EXPORTER_VERSION="1.6.0"
ARG MYSQL_CONNECTOR_VERSION="9.7.0"
ARG HUDI_BUNDLE_VERSION="1.2.0"

ARG USER="hive"
ARG USERID="1516"
ARG HADOOP_GROUP_NAME="hadoop"
ARG HADOOP_GROUP_ID="1234"

RUN apt-get update && apt-get upgrade -y

# Remove Canonical Pebble. Unused and its Go stdlib triggers CVE findings.
RUN rm -rf /usr/bin/pebble /var/lib/pebble

RUN groupadd -g ${HADOOP_GROUP_ID} ${HADOOP_GROUP_NAME} && \
    useradd -m --uid ${USERID} --gid ${HADOOP_GROUP_ID} ${USER}

# Download HADOOP
RUN --mount=type=secret,id=wgetrc,dst=/root/.wgetrc \
    mkdir -p /srv/hops && \
    wget -q https://nexus.hops.works/repository/hopshadoop/hops-${HADOOP_VERSION_EE}.tgz && \
    tar -C /srv/hops/ -zxf hops-$HADOOP_VERSION_EE.tgz && \
    ln -s /srv/hops/hadoop-$HADOOP_VERSION_EE /srv/hops/hadoop && \
    rm -rf /srv/hops/hadoop/etc && \
    rm -rf /srv/hops/hadoop/sbin && \
    rm hops-$HADOOP_VERSION_EE.tgz

# Make sure you build with --platform linux/amd64

# Java 17 compatibility symlink for scripts that expect a Debian-style JVM path.
RUN mkdir -p /usr/lib/jvm &&  ln -s $JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64
ENV HADOOP_HOME=/srv/hops/hadoop
ENV HADOOP_CONF_DIR="$HADOOP_HOME/conf"

# Copy pre-downloaded hive packaging artifact
COPY ${HIVE_TAR_NAME} /tmp/${HIVE_TAR_NAME}
RUN tar -C /srv/hops/ -zxf /tmp/${HIVE_TAR_NAME} && \
    ln -s /srv/hops/apache-hive-$HIVE_VERSION-bin /srv/hops/hive && \
    rm /tmp/${HIVE_TAR_NAME}

RUN rm -rf /srv/hops/hive/conf/*
RUN chmod -R 755 -R /srv/hops/apache-hive-$HIVE_VERSION-bin

ENV HIVE_HOME=/srv/hops/hive
ENV PATH="/srv/hops/super_crypto/hive:$HIVE_HOME/bin:/opt/venv/bin:$HADOOP_HOME/bin:$PATH"


# Download extra jars
ENV HOPSWORKS_JARS=/srv/hops/hopsworks-jars
RUN mkdir -p ${HOPSWORKS_JARS}
# https://repo.hops.works/master/prometheus/jmx_prometheus_javaagent-0.12.0.jar
RUN wget -qO ${HOPSWORKS_JARS}/jmx_prometheus_javaagent-${PROMETHEUS_EXPORTER_VERSION}.jar https://repo.hops.works/master/prometheus/jmx_prometheus_javaagent-${PROMETHEUS_EXPORTER_VERSION}.jar
# https://repo.hops.works/master/mysql-connector-java-8.0.21-bin.jar
RUN wget -qO ${HOPSWORKS_JARS}/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar https://repo.hops.works/master/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar

RUN chmod -R 755 ${HOPSWORKS_JARS}

RUN wget -qO ${HOPSWORKS_JARS}/hudi-hadoop-mr-bundle-${HUDI_BUNDLE_VERSION}.jar https://repo.hops.works/master/hudi/${HUDI_BUNDLE_VERSION}/hudi-hadoop-mr-bundle-${HUDI_BUNDLE_VERSION}.jar

ENV METASTORE_XMX=2g
ENV METASTORE_XMS=2g
ENV HOPSWORKS_JARS=/srv/hops/hopsworks-jars
ENV HIVE_METASTORE_HADOOP_OPTS="-Xmx${METASTORE_XMX} -Xms${METASTORE_XMS} -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=65 -javaagent:${HOPSWORKS_JARS}/jmx_prometheus_javaagent-${PROMETHEUS_EXPORTER_VERSION}.jar=18002:${HIVE_HOME}/conf/hivemetaserver.yaml -Djdk.tls.ephemeralDHKeySize=2048 -Dlog4j2.formatMsgNoLookups=true"
ENV HIVE_AUX_JARS_PATH=$HOPSWORKS_JARS
# ENV HIVESERVER2_HADOOP_OPTS='-Djava.io.tmpdir=/tmp/$HIVE_USER'


RUN mkdir -p /home/$USER
RUN chown $USER:${HADOOP_GROUP_NAME} /home/$USER

RUN mkdir -p $HIVE_HOME/scripts

COPY entrypoint.sh $HIVE_HOME/scripts/entrypoint.sh
RUN chmod -R 755 $HIVE_HOME/scripts && \
    mkdir -p $HIVE_HOME/conf && \
    chmod -R 777 $HIVE_HOME/conf && \
    mkdir -p $HIVE_HOME/logs && \
    chmod -R 777 $HIVE_HOME/logs

USER $USER

WORKDIR $HIVE_HOME

ENTRYPOINT [ "./scripts/entrypoint.sh" ]
