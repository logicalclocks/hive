#!/bin/bash

printenv

## Some env configuration
export CLASSPATH=`$HADOOP_HOME/bin/hdfs classpath --glob`

# e.g. hive --service metastore
exec "$@"

