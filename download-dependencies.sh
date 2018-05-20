#!/bin/sh

rm -rf app/src/main/jniLibs/
mkdir app/src/main/jniLibs/

mkdir -p app/src/main/jniLibs/armeabi-v7a
mkdir -p app/src/main/jniLibs/armeabi
mkdir -p app/src/main/jniLibs/x86
mkdir -p app/src/main/jniLibs/arm64-v8a
mkdir -p app/src/main/jniLibs/x86_64

find app/src/main/jniLibs/ -type f -name 'libtox4j*' -exec rm -f {} +


echo "Downloading native libraries for tox4j..."

wget https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so -O app/src/main/jniLibs/armeabi-v7a/libtox4j-c.so
wget https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so -O app/src/main/jniLibs/armeabi/libtox4j-c.so
wget https://build.tox.chat/job/tox4j_build_android_x86_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so -O app/src/main/jniLibs/x86/libtox4j-c.so
wget https://build.tox.chat/job/tox4j_build_android_arm64_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so -O app/src/main/jniLibs/arm64-v8a/libtox4j-c.so
wget https://build.tox.chat/job/tox4j_build_android_x86-64_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so -O app/src/main/jniLibs/x86_64/libtox4j-c.so


mkdir -p app/libs
rm -f app/libs/tox4j*.jar

echo "Downloading new verion of tox4j..."

wget https://build.tox.chat/job/tox4j-api_build_android_multiarch_release/lastSuccessfulBuild/artifact/tox4j-api/target/scala-2.11/tox4j-api_2.11-0.1.2.jar -O app/libs/tox4j-api-c.jar
wget https://build.tox.chat/job/tox4j_build_android_arm64_release/lastSuccessfulBuild/artifact/artifacts/tox4j-c_2.11-0.1.2-SNAPSHOT.jar -O app/libs/tox4j-c.jar





