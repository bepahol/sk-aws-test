#!/bin/ksh

cd `dirname $0`

if [[ -z $1 ]] ; then
    echo "USAGE: $0 <local|spark-url>"
fi

if [[ $1 == "local" ]] ; then
    ~/spark-2.3.1-bin-hadoop2.7/bin/spark-submit --class "SimpleApp"     --master local[4] ~/spark-2.3.1-bin-hadoop2.7/target/simple-project-1.0.jar
else
    ~/spark-2.3.1-bin-hadoop2.7/bin/spark-submit --class "SimpleAppSkfs" --master $1 local:/var/tmp/silverking/skfs/skfs_mnt/skfs/simple-project-1.0.jar --deploy-mode cluster
fi

