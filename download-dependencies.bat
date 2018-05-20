@echo off

rmdir /s /q app\src\main\jniLibs\
mkdir app\src\main\jniLibs\

mkdir app\src\main\jniLibs\armeabi-v7a
mkdir app\src\main\jniLibs\armeabi
mkdir app\src\main\jniLibs\x86
mkdir app\src\main\jniLibs\arm64-v8a
mkdir app\src\main\jniLibs\x86_64

mkdir app\libs


echo Removed old version

powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so', 'app\src\main\jniLibs\armeabi-v7a\libtox4j-c.so')"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so', 'app\src\main\jniLibs\armeabi\libtox4j-c.so')"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_x86_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so', 'app\src\main\jniLibs\x86\libtox4j-c.so')"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_arm64_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so', 'app\src\main\jniLibs\arm64-v8a\libtox4j-c.so')"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_x86-64_release/lastSuccessfulBuild/artifact/artifacts/libtox4j-c.so', 'app\src\main\jniLibs\x86_64\libtox4j-c.so')"

DEL /F /Q app\libs\tox4j*
echo Removed old version: tox4j
echo Downloading latest version: tox4j
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/tox4j-c_2.11-0.1.2-SNAPSHOT.jar', 'app\libs\tox4j-c.jar')"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j-api_build_android_multiarch_release/lastSuccessfulBuild/artifact/tox4j-api/target/scala-2.11/tox4j-api_2.11-0.1.2.jar', 'app\libs\tox4j-api.jar')"

echo Downloaded.

echo ...Finished!
pause
@echo ON
