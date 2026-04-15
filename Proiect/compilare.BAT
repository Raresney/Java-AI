@echo off
REM Compilare proiect. Fara IDE - doar javac din JDK.
REM Cale JDK detectata pe sistem (Eclipse Adoptium JDK 17).

set JDK="C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\javac.exe"
set OPENCV_JAR=C:\Users\bighi\Downloads\opencv\build\java\opencv-4100.jar
set SRC=src

REM Verificam ca javac exista.
if not exist %JDK% (
    echo EROARE: javac nu exista la %JDK%. Corecteaza path-ul in compilare.BAT.
    exit /b 1
)

REM Listam toate fisierele .java recursiv.
dir /s /b %SRC%\*.java > sources.txt

REM Cream classpath-ul conditional.
if exist "%OPENCV_JAR%" (
    set CP=%SRC%;%OPENCV_JAR%
    echo OpenCV gasit la %OPENCV_JAR%.
) else (
    set CP=%SRC%
    echo ATENTIE: OpenCV NU este instalat. Se compileaza fara stratul webcam.
)

%JDK% -encoding UTF-8 -classpath "%CP%" -d %SRC% @sources.txt > erori.txt 2>&1
set JAVAC_RESULT=%ERRORLEVEL%
del sources.txt

if %JAVAC_RESULT% EQU 0 (
    echo Compilare reusita. Vezi erori.txt pentru avertismente.
) else (
    echo EROARE de compilare [cod %JAVAC_RESULT%]. Vezi erori.txt.
)
