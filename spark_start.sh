#!/bin/ksh

cd `dirname $0`

function f_spark_downloadSpark {
    cd ~
    wget http://mirror.cc.columbia.edu/pub/software/apache/spark/spark-2.3.1/spark-2.3.1-bin-hadoop2.7.tgz
    tar -xvf spark-2.3.1-bin-hadoop2.7.tgz
}

function f_spark_scpEnvToSlaves {
    print "Copying spark-env to all slaves"
    
    typeset  srcDir=~/spark-2.3.1-bin-hadoop2.7/conf/
    typeset destDir=$srcDir
    typeset    file=$srcDir/spark-env.sh
    
    f_spark_scp_helper "$destDir" "$file"
}

function f_spark_scp_helper {
    typeset destDir=$1
    typeset    file=$2
    
    while read host; do
        echo -n "$host: "
        scp -o StrictHostKeyChecking=no $file $USER@$host:$destDir
    done < $NONLAUNCH_HOST_LIST_FILENAME
}

print "PREPPING MASTER MACHINE"
# f_spark_downloadSpark
# export JAVA_HOME=/usr/lib/jvm/java-1.8.0
~/spark-2.3.1-bin-hadoop2.7/sbin/start-master.sh

print "PREPPING SLAVE MACHINES"
cp ~/SilverKing/build/aws/multi_nonlaunch_machines_list.txt ~/spark-2.3.1-bin-hadoop2.7/conf/slaves
# echo "export JAVA_HOME=/usr/lib/jvm/java-1.8.0" > ~/spark-2.3.1-bin-hadoop2.7/conf/spark-env.sh
# f_spark_downloadSparkOnSlaves
# f_spark_scpEnvToSlaves
~/spark-2.3.1-bin-hadoop2.7/sbin/start-slaves.sh




