@echo off
setlocal enabledelayedexpansion

REM Adjust these two paths to match your machine
set "WTK_HOME=H:\WTK2.5.2"
set "JDK_BIN=C:\Program Files\Java\jdk1.8.0_202\bin"

set "JAVAC=%JDK_BIN%\javac.exe"
set "JAR=%JDK_BIN%\jar.exe"
set "PREVERIFY=%WTK_HOME%\bin\preverify.exe"
set "BOOTCP=%WTK_HOME%\lib\cldcapi11.jar;%WTK_HOME%\lib\midpapi20.jar;%WTK_HOME%\lib\mmapi.jar"

if not exist "%JAVAC%" (
    echo ERROR: javac.exe not found at "%JAVAC%"
    exit /b 1
)
if not exist "%PREVERIFY%" (
    echo ERROR: preverify.exe not found at "%PREVERIFY%"
    exit /b 1
)
if not exist "res\data.pak" (
    echo ERROR: res\data.pak is missing. Run "python tools/pack_data.py" first.
    exit /b 1
)

for %%D in (build\classes build\preverified build\pkg dist) do (
    if exist "%%D" rmdir /s /q "%%D"
    mkdir "%%D"
)

type nul > build\sources.txt
for /r "src" %%F in (*.java) do (
    set "SRC_PATH=%%F"
    set "SRC_PATH=!SRC_PATH:\=/!"
    echo "!SRC_PATH!">> build\sources.txt
)

echo Compiling CLDC 1.1 / MIDP 2.0 sources...
"%JAVAC%" -source 1.3 -target 1.1 -bootclasspath "%BOOTCP%" -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1

echo Preverifying bytecode...
"%PREVERIFY%" -classpath "%BOOTCP%" -d build\preverified build\classes
if errorlevel 1 exit /b 1

echo Staging JAR contents...
xcopy /e /i /y /q build\preverified\* build\pkg\ >nul
copy /y res\data.pak build\pkg\data.pak >nul


mkdir build\pkg\img
xcopy /y /q res\img\*.png build\pkg\img\ >nul
if exist build\pkg\img\Editor.png del /q build\pkg\img\Editor.png
if exist build\pkg\img\NpcType.png del /q build\pkg\img\NpcType.png

if exist res\audio (
    xcopy /e /i /y /q res\audio build\pkg\audio\ >nul
)

(
    echo Manifest-Version: 1.0
    echo MIDlet-Name: Ikachan
    echo MIDlet-Version: 1.0.0
    echo MIDlet-Vendor: Studio Pixel / Port
    echo MIDlet-1: Ikachan, , ikachan.IkachanMIDlet
    echo MicroEdition-Profile: MIDP-2.0
    echo MicroEdition-Configuration: CLDC-1.1
    echo.
) > build\MANIFEST.MF

echo Packaging dist\Ikachan.jar...
"%JAR%" cfm dist\Ikachan.jar build\MANIFEST.MF -C build\pkg .
if errorlevel 1 exit /b 1

for %%A in ("dist\Ikachan.jar") do set "JAR_SIZE=%%~zA"

(
    echo MIDlet-Name: Ikachan
    echo MIDlet-Version: 1.0.0
    echo MIDlet-Vendor: Studio Pixel / Port
    echo MIDlet-1: Ikachan, , ikachan.IkachanMIDlet
    echo MIDlet-Jar-URL: Ikachan.jar
    echo MIDlet-Jar-Size: !JAR_SIZE!
    echo MicroEdition-Profile: MIDP-2.0
    echo MicroEdition-Configuration: CLDC-1.1
) > dist\Ikachan.jad

echo Built dist\Ikachan.jar (!JAR_SIZE! bytes) and dist\Ikachan.jad
endlocal