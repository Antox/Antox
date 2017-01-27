#!/bin/sh
mkdir -p app/src/main/jniLibs/armeabi
mkdir -p app/src/main/jniLibs/x86
mkdir -p app/src/main/jniLibs/arm64-v8a

rm -f app/src/main/jniLibs/armeabi/libtox4j.so
rm -f app/src/main/jniLibs/x86/libtox4j.so
rm -f app/src/main/jniLibs/arm64-v8a/libtox4j.so

rm -f app/src/main/jniLibs/armeabi/libkaliumjni.so
rm -f app/src/main/jniLibs/x86/libkaliumjni.so
rm -f app/src/main/jniLibs/arm64-v8a/libkaliumjni.so

cd ${0%/*}
mkdir -p app/libs
rm -f app/libs/tox4j*.jar

REPOUSER="zoff99"
REPO="Antox"
BRANCH="zoff99%2FAntox_v0.25.1"

wget 'https://circleci.com/api/v1/project/'"$REPOUSER"'/'"$REPO"'/latest/artifacts/0/$CIRCLE_ARTIFACTS/supplement.zip?filter=successful&branch='"$BRANCH" -O ./supplement.zip
unzip -o ./supplement.zip

git submodule init
git submodule update
