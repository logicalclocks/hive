FROM openjdk:8
ARG HIVE_VERSION=3.0.0.13.3
ARG HADOOP_VERSION_EE=3.2.0.12-EE-SNAPSHOT
ARG PROMETHEUS_EXPORTER_VERSION="0.12.0"
ARG USER="hive"
ARG USERID="1516"
ARG HADOOP_GROUP_ID="1234"
ARG METASERVER_ID="1505"
ARG PROMETHEUS_EXPORTER_VERSION="0.12.0"
ARG MYSQL_CONNECTOR_VERSION="8.0.21"
ARG HUDI_BUNDLE_VERSION="0.12.3.0"

# This is for local dev in the VM
RUN useradd -m --uid $USERID  $USER

RUN groupadd -g ${METASERVER_ID} metaserver && \
    usermod -a -G metaserver $USER
RUN groupadd -g ${HADOOP_GROUP_ID} hadoop && \
    usermod -a -G hadoop $USER
    


# Download HADOOP
RUN --mount=type=secret,id=wgetrc,dst=/root/.wgetrc \
    mkdir -p /srv/hops && \
    wget https://nexus.hops.works/repository/hopshadoop/hops-$HADOOP_VERSION_EE.tgz && \
    tar -C /srv/hops/ -zxf hops-$HADOOP_VERSION_EE.tgz && \
    ln -s /srv/hops/hadoop-$HADOOP_VERSION_EE /srv/hops/hadoop && \
    rm -rf /srv/hops/hadoop/etc && \
    rm -rf /srv/hops/hadoop/sbin && \
    rm hops-$HADOOP_VERSION_EE.tgz

# Make sure you build with --platform linux/amd64

# /usr/local/openjdk-8/bin/java
ENV JAVA_HOME=/usr/local/openjdk-8
RUN mkdir -p /usr/lib/jvm &&  ln -s $JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
ENV HADOOP_HOME=/srv/hops/hadoop
ENV HADOOP_CONF_DIR="$HADOOP_HOME/conf"

# Download hive
# https://archiva.hops.works/repository/Hops/io/hops/hive/hive-packaging/3.0.0.13.3/hive-packaging-3.0.0.13.3-bin.tar.gz
RUN wget https://archiva.hops.works/repository/Hops/io/hops/hive/hive-packaging/$HIVE_VERSION/hive-packaging-$HIVE_VERSION-bin.tar.gz && \
    tar -C /srv/hops/ -zxf hive-packaging-$HIVE_VERSION-bin.tar.gz && \
    ln -s /srv/hops/apache-hive-$HIVE_VERSION-bin /srv/hops/hive && \
    rm hive-packaging-$HIVE_VERSION-bin.tar.gz


ENV HIVE_HOME=/srv/hops/hive
ENV PATH="/srv/hops/super_crypto/hive:$HIVE_HOME/bin:/opt/venv/bin:$HADOOP_HOME/bin:$PATH"


# Download extra jars
ENV HOPSWORKS_JARS=/srv/hops/hopsworks-jars
RUN mkdir -p ${HOPSWORKS_JARS}
# https://repo.hops.works/master/prometheus/jmx_prometheus_javaagent-0.12.0.jar
RUN wget -O ${HOPSWORKS_JARS}/jmx_prometheus_javaagent-${PROMETHEUS_EXPORTER_VERSION}.jar https://repo.hops.works/master/prometheus/jmx_prometheus_javaagent-${PROMETHEUS_EXPORTER_VERSION}.jar
# https://repo.hops.works/master/mysql-connector-java-8.0.21-bin.jar
RUN wget -O ${HOPSWORKS_JARS}/mysql-connector-java-${MYSQL_CONNECTOR_VERSION}-bin.jar https://repo.hops.works/master/mysql-connector-java-${MYSQL_CONNECTOR_VERSION}-bin.jar

RUN wget -O ${HOPSWORKS_JARS}/hudi-hadoop-mr-bundle-$HUDI_BUNDLE_VERSION.jar  http://archiva.hops.works/repository/Hops/io/hops/hudi/hudi-hadoop-mr-bundle/$HUDI_BUNDLE_VERSION/hudi-hadoop-mr-bundle-$HUDI_BUNDLE_VERSION.jar


ENV METASTORE_XMX=2g
ENV METASTORE_XMS=2g
ENV HOPSWORKS_JARS=/srv/hops/hopsworks-jars
ENV HIVE_METASTORE_HADOOP_OPTS="-Xmx${METASTORE_XMX} -Xms${METASTORE_XMS} -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=65 -XX:+UseCMSInitiatingOccupancyOnly -javaagent:${HOPSWORKS_JARS}/jmx_prometheus_javaagent-${PROMETHEUS_EXPORTER_VERSION}.jar=18002:${HIVE_HOME}/conf/hivemetaserver.yaml -Djdk.tls.ephemeralDHKeySize=2048 -Dlog4j2.formatMsgNoLookups=true"
ENV HIVE_AUX_JARS_PATH=$HOPSWORKS_JARS
# ENV HIVESERVER2_HADOOP_OPTS='-Djava.io.tmpdir=/tmp/$HIVE_USER'


RUN mkdir -p /home/$USER
RUN chown $USER:$GROUP /home/$USER

WORKDIR /home/$USER
COPY dockerfiles/entrypoint.sh entrypoint.sh
RUN chown $USER:$GROUP  entrypoint.sh 
RUN chown -R $USER:$GROUP $HIVE_HOME
USER $USER

ENTRYPOINT [ "./entrypoint.sh" ]
