#!/bin/bash
mkdir -p app/src/main/jniLibs/armeabi
rm app/src/main/jniLibs/armeabi/libtox4j.so
wget https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/armeabi/libtox4j.so -O app/src/main/jniLibs/armeabi/libtox4j.so
cd ${0%/*}
mkdir -p app/libs
rm app/libs/tox4j_2.11.jar
wget https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/tox4j_2.11-0.1-SNAPSHOT.jar -O app/libs/tox4j_2.11.jar
rm app/libs/protobuf-java-2.6.1.jar
wget https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/protobuf.jar -O app/libs/protobuf-java-2.6.1.jar
