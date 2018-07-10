#!/bin/ksh

if [[ -z $1 ]] ; then
    echo "USAGE: $0 <local|spark-url>"
fi

if [[ $1 == "local" ]] ; then
    ./bin/spark-submit --class "SimpleApp"     --master local[4] target/simple-project-1.0.jar
else
    ./bin/spark-submit --class "SimpleAppSkfs" --master $1 local:/var/tmp/silverking/skfs/skfs_mnt/skfs/simple-project-1.0.jar --deploy-mode cluster
fi

