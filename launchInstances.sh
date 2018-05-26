#!/bin/ksh

if [[ -z $1 ]]; then
    echo "Please pass in <numInstances>"
    exit
fi

file_path=com/ms/silverking/aws/MultiInstanceLauncher

cd src/
/usr/lib/jvm/java-1.8.0/bin/javac -cp .:../lib/aws-java-sdk-1.11.333/lib/* $file_path.java
/usr/lib/jvm/java-1.8.0/bin/java  -cp .:../lib/aws-java-sdk-1.11.333/lib/*:../lib/aws-java-sdk-1.11.333/third-party/lib/* $file_path $1