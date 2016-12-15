@echo off
echo Started...

mkdir app\src\main\jniLibs\armeabi
mkdir app\src\main\jniLibs\x86
mkdir app\src\main\jniLibs\arm64-v8a

DEL /F /Q app\src\main\jniLibs\armeabi\libtox4j.so
DEL /F /Q app\src\main\jniLibs\x86\libtox4j.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libtox4j.so

REM echo Downloading latest version: libtox4j.so
REM powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so', 'app\src\main\jniLibs\armeabi\libtox4j.so')"
REM powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_x86_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so', 'app\src\main\jniLibs\x86\libtox4j.so')"
REM powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_arm64_release/lastSuccessfulBuild/artifact/artifacts/libtox4j.so', 'app\src\main\jniLibs\arm64-v8a\libtox4j.so')"

DEL /F /Q app\src\main\jniLibs\armeabi\libkaliumjni.so
DEL /F /Q app\src\main\jniLibs\x86\libkaliumjni.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libkaliumjni.so

REM echo Downloading latest version: libkaliumjni.so
REM powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/libkaliumjni_build_android_armel_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so', 'app\src\main\jniLibs\armeabi\libkaliumjni.so')"
REM powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/libkaliumjni_build_android_x86_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so', 'app\src\main\jniLibs\x86\libkaliumjni.so')"
REM powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/libkaliumjni_build_android_arm64_static_release/lastSuccessfulBuild/artifact/kalium-jni/jni/libkaliumjni.so', 'app\src\main\jniLibs\arm64-v8a\libkaliumjni.so')"

mkdir app\libs
DEL /F /Q app\libs\tox4j_2.11.jar
echo Removed old version: tox4j_2.11.jar
REM echo Downloading latest version: tox4j_2.11.jar
REM powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_armel_release/lastSuccessfulBuild/artifact/artifacts/tox4j_2.11-0.1-SNAPSHOT.jar', 'app\libs\tox4j_2.11.jar')"
REM echo Downloaded.
REM echo ...Finished!
pause
@echo ON
