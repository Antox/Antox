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

pwd
mkdir -p app/libs
rm -f app/libs/tox4j*.jar

# default values ------------
# REPOUSER="zoff99"
# REPO="Antox"
# BRANCH="zoff99%2FAntox_v0.25.1"
# default values ------------

# get values from git commandline ------------
BRANCH=$(git rev-parse --abbrev-ref HEAD|sed -e 's# #%20#g'|sed -e 'sx#x%23xg'|sed -e 's#/#%2F#g'| grep -v HEAD || git name-rev --name-only HEAD|sed -e 's#^remotes/origin/##'|sed -e 's#^origin/##'|sed -e 's# #%20#g'|sed -e 'sx#x%23xg'|sed -e 's#/#%2F#g')
REPO="Antox"
REPOUSER=$(git config --get remote.origin.url|cut -d'/' -f 4)
# get values from git commandline ------------

echo $BRANCH
echo $REPO
echo $REPOUSER

wget 'https://circleci.com/api/v1/project/'"$REPOUSER"'/'"$REPO"'/latest/artifacts/0/$CIRCLE_ARTIFACTS/supplement.zip?filter=successful&branch='"$BRANCH" -O ./supplement.zip
unzip -o ./supplement.zip
ls -al ./supplement.zip
rm -f ./supplement.zip

mkdir -p app/src/main/java/org/
pushd app/src/main/java/org/
ln -sf ../../../../../libsodium-jni/src/main/java/org/libsodium
popd

sed -i -e 's#^.*com.github.joshjdevl.libsodiumjni:libsodium-jni-aar.*$##' app/build.gradle

git submodule init
git submodule update
