@echo off
setlocal
set "GSON=src\gson-2.10.1.jar"
set "SRC=src\Test.java"
set "OUT=out"
if not exist "%OUT%" mkdir "%OUT%"
echo Compiling...
javac -cp "%GSON%" -d "%OUT%" %SRC%
if errorlevel 1 (
  echo Compilation failed.
  exit /b 1
)
echo Compilation succeeded.
exit /b 0
