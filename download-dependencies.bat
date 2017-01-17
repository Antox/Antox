@echo off

mkdir app\src\main\jniLibs\armeabi
mkdir app\src\main\jniLibs\x86
mkdir app\src\main\jniLibs\arm64-v8a

DEL /F /Q app\src\main\jniLibs\armeabi\libtox4j.so
DEL /F /Q app\src\main\jniLibs\x86\libtox4j.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libtox4j.so

DEL /F /Q app\src\main\jniLibs\armeabi\libkaliumjni.so
DEL /F /Q app\src\main\jniLibs\x86\libkaliumjni.so
DEL /F /Q app\src\main\jniLibs\arm64-v8a\libkaliumjni.so


mkdir app\libs
DEL /F /Q app\libs\tox4j*.jar
echo Removed old version
echo Downloading latest version ...
REM REPOUSER="zoff99"
REM REPO="Antox"
REM BRANCH="z_new_source"
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://circleci.com/api/v1/project/zoff99/Antox/latest/artifacts/0/$CIRCLE_ARTIFACTS/and_stud_prj.zip?filter=successful&branch=z_new_source', 'and_stud_prj.zip')"
echo Downloaded.
echo unzipping
unzip and_stud_prj.zip
echo ...Finished!
pause
@echo ON
