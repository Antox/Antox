#!/bin/bash
mkdir -p app/src/main/jniLibs/armeabi
rm app/src/main/jniLibs/armeabi/libtox4j.so
wget https://jenkins.libtoxcore.so/job/tox4j-android-arm/lastSuccessfulBuild/artifact/artifacts/armeabi/libtox4j.so -O app/src/main/jniLibs/armeabi/libtox4j.so
cd ${0%/*}
mkdir -p app/libs
rm app/libs/tox4j_2.11.jar
wget https://jenkins.libtoxcore.so/job/tox4j-android-arm/lastSuccessfulBuild/artifact/artifacts/tox4j_2.11-0.0.0-SNAPSHOT.jar -O app/libs/tox4j_2.11.jar
rm app/libs/protobuf-java-2.6.1.jar
wget https://jenkins.libtoxcore.so/job/tox4j-android-arm/lastSuccessfulBuild/artifact/artifacts/protobuf.jar -O app/libs/protobuf-java-2.6.1.jar
