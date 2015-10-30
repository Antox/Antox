#!/bin/bash
APPNAME='antox'
mkdir -p $APPNAME/src/main/jniLibs/armeabi
rm $APPNAME/src/main/jniLibs/armeabi/libtox4j.so
wget https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/armeabi/libtox4j.so -O $APPNAME/src/main/jniLibs/armeabi/libtox4j.so
cd ${0%/*}
mkdir -p $APPNAME/libs
rm $APPNAME/libs/tox4j_2.11.jar
wget https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/tox4j_2.11-0.1-SNAPSHOT.jar -O $APPNAME/libs/tox4j_2.11.jar
rm $APPNAME/libs/protobuf-java-2.6.1.jar
wget https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/protobuf.jar -O $APPNAME/libs/protobuf-java-2.6.1.jar
