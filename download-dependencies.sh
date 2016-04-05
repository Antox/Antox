#!/bin/sh
mkdir -p app/src/main/jniLibs/armeabi
mkdir -p app/src/main/jniLibs/x86
mkdir -p app/src/main/jniLibs/arm64-v8a

rm app/src/main/jniLibs/armeabi/libtox4j.so
rm app/src/main/jniLibs/x86/libtox4j.so
rm app/src/main/jniLibs/arm64-v8a/libtox4j.so

wget https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so -O app/src/main/jniLibs/armeabi/libtox4j.so
wget https://build.tox.chat/job/tox4j_build_android_x86_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so -O app/src/main/jniLibs/x86/libtox4j.so
wget https://build.tox.chat/job/tox4j_build_android_arm64_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so -O app/src/main/jniLibs/arm64-v8a/libtox4j.so

sha256sum -c <<EOF
66259d0af988820f4df162775c9b27ebf65399f05ee409135d233b960a042eb3  app/src/main/jniLibs/x86/libtox4j.so
327430e7d13fa9e7e2ff767bc191b0fd0ddfa9766f16dd775ba11c238161ba2c  app/src/main/jniLibs/armeabi/libtox4j.so
5be537e297cfa560a3c11137d166dd74b2660cf1d81e5ad1bb0e71e11162279f  app/src/main/jniLibs/arm64-v8a/libtox4j.so
EOF
if [ $? -ne 0 ]; then
	echo checksum mismatch
	exit 1
fi

rm app/src/main/jniLibs/armeabi/libkaliumjni.so
rm app/src/main/jniLibs/x86/libkaliumjni.so
rm app/src/main/jniLibs/arm64-v8a/libkaliumjni.so

wget https://build.tox.chat/job/libkaliumjni_build_android_armel_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so -O app/src/main/jniLibs/armeabi/libkaliumjni.so
wget https://build.tox.chat/job/libkaliumjni_build_android_x86_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so -O app/src/main/jniLibs/x86/libkaliumjni.so
wget https://build.tox.chat/job/libkaliumjni_build_android_arm64_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so -O app/src/main/jniLibs/arm64-v8a/libkaliumjni.so

sha256sum -c <<EOF
369b9edbe3489b7b60ab593f9da3a20ec94b72cd042b6b0b8973f09f507aec59  app/src/main/jniLibs/x86/libkaliumjni.so
b94fe53ae10202e0024b7fa347529cd2e49537a38b6b5514d562a8fb1013c406  app/src/main/jniLibs/armeabi/libkaliumjni.so
1ef49b2abbd41344d08c4c50301ba8f872ed8f810942fba6a146996492cc7751  app/src/main/jniLibs/arm64-v8a/libkaliumjni.so
EOF
if [ $? -ne 0 ]; then
	echo checksum mismatch
	exit 1
fi

cd ${0%/*}
mkdir -p app/libs
rm app/libs/tox4j_2.11.jar
wget https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/tox4j_2.11-0.1-SNAPSHOT.jar -O app/libs/tox4j_2.11.jar

sha256sum -c <<EOF
81ee3d9cc7e0847ea3dafed0366ac7be87c28cd13d4395aaf791128a5bf59f5d  app/libs/tox4j_2.11.jar
EOF
if [ $? -ne 0 ]; then
	echo checksum mismatch
	exit 1
fi
