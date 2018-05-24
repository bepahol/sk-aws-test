#!/bin/ksh

if [[ -z $1 ]]; then
    echo "Please pass in <numInstances>"
    exit
fi

file_path=com/ms/silverking/aws/MultiInstanceLauncher

cd src/
javac -cp .:../lib/aws-java-sdk-1.11.333/lib/* $file_path.java
java  -cp .:../lib/aws-java-sdk-1.11.333/lib/*:../lib/aws-java-sdk-1.11.333/third-party/lib/* $file_path $1