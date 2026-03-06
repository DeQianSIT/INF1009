@echo off
REM Abstract Engine Build Script for Windows

echo ======================================
echo   Building Abstract Engine
echo ======================================
echo.

REM Create bin directory if it doesn't exist
if not exist bin mkdir bin
if %errorlevel% == 0 echo Created bin directory

echo Compiling Java files...
cd src

REM Compile all Java files
dir /s /B *.java > sources.txt
javac -d ../bin @sources.txt

if %errorlevel% == 0 (
    echo.
    echo [SUCCESS] Compilation successful!
    echo.
    echo To run the simulation:
    echo   cd bin
    echo   java simulation.EngineSimulation
    echo.
    
    del sources.txt
    
    set /p RUN="Run the simulation now? (y/n): "
    if /i "%RUN%"=="y" (
        cd ..\bin
        java simulation.EngineSimulation
    )
) else (
    echo.
    echo [ERROR] Compilation failed!
    del sources.txt
    exit /b 1
)

cd ..
