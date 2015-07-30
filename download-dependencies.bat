@echo off
echo "Started..."
mkdir app\src\main\jniLibs\armeabi
DEL /F /Q app\src\main\jniLibs\armeabi\libtox4j.so
echo "Removed old version: libtox4j.so"
echo "Downloading latest version: libtox4j.so"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/armeabi/libtox4j.so', 'app\src\main\jniLibs\armeabi\libtox4j.so')"
echo "Downloaded."
mkdir app\libs
DEL /F /Q app\libs\tox4j_2.11.jar
echo "Removed old version: tox4j_2.11.jar"
echo "Downloading latest version: tox4j_2.11.jar"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/tox4j_2.11-0.1-SNAPSHOT.jar', 'app\libs\tox4j_2.11.jar')"
echo "Downloaded."
DEL /F /Q app\libs\protobuf-java-2.6.1.jar
echo "Removed old version: protobuf-java-2.6.1.jar"
echo "Downloading latest version: protobuf-java-2.6.1.jar"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://build.tox.chat/job/tox4j_build_android_arm_release/lastSuccessfulBuild/artifact/artifacts/protobuf.jar', 'app\libs\protobuf-java-2.6.1.jar')"
echo "Downloaded."
echo "...Finished!"
@echo ON
