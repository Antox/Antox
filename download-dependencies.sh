#!/bin/bash
cd ${0%/*}
mkdir -p app/src/main/jniLibs/armeabi
cd app/src/main/jniLibs/armeabi
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/armeabi/libjtoxcore.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/armeabi/libsodium.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/armeabi/libtoxcore.so
cd ${0%/*}
mkdir -p app/src/main/jniLibs/armeabi-v7a
cd app/src/main/jniLibs/armeabi-v7a
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/armeabi-v7a/libjtoxcore.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/armeabi-v7a/libsodium.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/armeabi-v7a/libtoxcore.so
cd ${0%/*}
mkdir -p app/src/main/jniLibs/mips
cd app/src/main/jniLibs/mips
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/mips/libjtoxcore.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/mips/libsodium.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/mips/libtoxcore.so
cd ${0%/*}
mkdir -p app/src/main/jniLibs/x86
cd app/src/main/jniLibs/x86
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/x86/libjtoxcore.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/x86/libsodium.so
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/x86/libtoxcore.so
cd ${0%/*}
mkdir -p app/libs
cd app/libs
wget https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/artifacts/jToxcore.jar
