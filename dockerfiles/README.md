# Docker image for Hive metastore service

This folder contains the files we copy to the image when building

## Important folders inside the container (maybe for mounting)

- logs: /srv/hops/hive/logs
- conf: /srv/hops/hive/conf

## Build args

**Note** we are downloading the hive packaging from the hopsworks [archiva](https://archiva.hops.works/#welcome). This is way easier than building inside docker.

- User: `hive`
- HIVE_VERSION: `3.0.0.13.10-SNAPSHOT`. This should come automatically from reading the pom file
- HADOOP_VERSION_EE: `3.2.0.17-EE-RC1`
- PROMETHEUS_EXPORTER_VERSION: '0.12.0'
- MYSQL_CONNECTOR_VERSION: '8.0.21'
- HUDI_BUNDLE_VERSION: '0.12.3.0'


## How to build

- From the `dockerfiles` folder
- Create the secret `printf "user=<user>\npassword=<password>" > wgetrc` to download hadoop.
- Run `docker build -m 12g -c 32 -f Dockerfile ../  --secret id=wgetrc,src=wgetrc  --platform linux/amd64 -t hopsworks/hive`
- Save the image `docker save hopsworks/hive > hive.tar`

## Set the environment for running

- Create a devconf folder and copy there the `/srv/hops/apache-hive/conf` folder. The hive-env.sh might be in conflict with the env of the Docker container, thus, it might be better to avoid copying it.
- There might be issues by mounting the certs and the users/groups of the container. To solve this, change the ownership of those so the container can run.


## Run container in dev env

- Stop the hivemetastore service in the vm `systemctl stop hivemetastore`

- Start the docker container 
``` bash
docker run -it --rm --name hive-metastore --network "host" --hostname "0.0.0.0" --memory=8G \
-v /srv/hops/hadoop/etc:/srv/hops/hadoop/etc -v $PWD/devconf:/srv/hops/hive/conf   -v  /srv/hops/super_crypto/hive:/srv/hops/super_crypto/hive \
hopsworks/hive hive --service metastore
```

