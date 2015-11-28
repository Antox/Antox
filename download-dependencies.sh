#!/bin/bash
mkdir -p app/src/main/jniLibs/armeabi
mkdir -p app/src/main/jniLibs/x86
mkdir -p app/src/main/jniLibs/arm64-v8a

rm app/src/main/jniLibs/armeabi/libtox4j.so
rm app/src/main/jniLibs/x86/libtox4j.so
rm app/src/main/jniLibs/arm64-v8a/libtox4j.so

wget https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so -O app/src/main/jniLibs/armeabi/libtox4j.so
wget https://build.tox.chat/job/tox4j_build_android_x86_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so -O app/src/main/jniLibs/x86/libtox4j.so
wget https://build.tox.chat/job/tox4j_build_android_arm64_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so -O app/src/main/jniLibs/arm64-v8a/libtox4j.so

rm app/src/main/jniLibs/armeabi/libkaliumjni.so
rm app/src/main/jniLibs/x86/libkaliumjni.so
rm app/src/main/jniLibs/arm64-v8a/libkaliumjni.so

wget https://build.tox.chat/job/libkaliumjni_build_android_armel_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so -O app/src/main/jniLibs/armeabi/libkaliumjni.so
wget https://build.tox.chat/job/libkaliumjni_build_android_x86_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so -O app/src/main/jniLibs/x86/libkaliumjni.so
wget https://build.tox.chat/job/libkaliumjni_build_android_arm64_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so -O app/src/main/jniLibs/arm64-v8a/libkaliumjni.so

cd ${0%/*}
mkdir -p app/libs
rm app/libs/tox4j_2.11.jar
wget https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/tox4j_2.11-0.1-SNAPSHOT.jar -O app/libs/tox4j_2.11.jar
rm app/libs/protobuf-java-2.6.1.jar
wget https://build.tox.chat/job/protobuf_build_android_armel_release/lastSuccessfulBuild/artifact/protobuf.jar -O app/libs/protobuf-java-2.6.1.jar

