@echo off
REM Lansare aplicatie. Adauga -Djava.library.path pentru DLL-ul nativ OpenCV.

set JDK="C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe"
set OPENCV_JAR=%USERPROFILE%\Downloads\opencv\build\java\opencv-4100.jar
set OPENCV_DLL_DIR=%USERPROFILE%\Downloads\opencv\build\java\x64

if not exist %JDK% (
    echo EROARE: java nu exista la %JDK%.
    exit /b 1
)

%JDK% -Xmx1024m -Djava.library.path="%OPENCV_DLL_DIR%" -classpath "src;%OPENCV_JAR%" proiect.Main > log.txt 2>&1
echo Rezultatul este in log.txt.
