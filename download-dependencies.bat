@echo off

mkdir app\src\main\jniLibs\armeabi
mkdir app\src\main\jniLibs\x86
mkdir app\src\main\jniLibs\arm64-v8a

DEL /F /Q app\src\main\jniLibs\armeabi\libtox4j.so
DEL /F /Q app\src\main\jniLibs\x86\libtox4j.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libtox4j.so

DEL /F /Q app\src\main\jniLibs\armeabi\libtox4j-c.so
DEL /F /Q app\src\main\jniLibs\x86\libtox4j-c.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libtox4j-c.so

DEL /F /Q app\src\main\jniLibs\armeabi\libkaliumjni.so
DEL /F /Q app\src\main\jniLibs\x86\libkaliumjni.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libkaliumjni.so

DEL /F /Q app\src\main\jniLibs\armeabi\libsodiumjni.so
DEL /F /Q app\src\main\jniLibs\x86\libsodiumjni.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libsodiumjni.so

REM remove the symlink (which is used only on unix systems)
DEL /F /S /Q app\src\main\java\org\libsodium\
DEL /F /Q app\src\main\java\org\libsodium


mkdir app\libs
DEL /F /Q app\libs\tox4j-api_2.11-0.1.1.jar
DEL /F /Q app\libs\tox4j-c_2.11.jar

echo Removed old version
echo Downloading latest version ...
REM REPOUSER="zoff99"
REM REPO="Antox"
REM BRANCH="zoff99%2FAntox_v0.25.1"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://circleci.com/api/v1/project/zoff99/Antox/latest/artifacts/0/$CIRCLE_ARTIFACTS/and_stud_prj.zip?filter=successful&branch=zoff99%%2FAntox_v0.25.1', 'and_stud_prj.zip')"
echo Downloaded.
echo unzipping
unzip and_stud_prj.zip
echo ...Finished!
pause
@echo ON
