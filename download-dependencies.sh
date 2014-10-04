#!/bin/bash
mkdir -p app/src/main/jniLibs/armeabi
rm app/src/main/jniLibs/armeabi/libtoxcore.so
wget https://jenkins.libtoxcore.so/job/jtoxcore_android_arm/lastSuccessfulBuild/artifact/artifacts/armeabi/libjtoxcore.so -O app/src/main/jniLibs/armeabi/libjtoxcore.so
cd ${0%/*}
mkdir -p app/libs
rm app/libs/jToxcore.jar
wget https://jenkins.libtoxcore.so/job/jtoxcore_android_arm/lastSuccessfulBuild/artifact/artifacts/jToxcore.jar -O app/libs/jToxcore.jar
