@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

SET BASE_DIR=%~dp0
SET WRAPPER_PROPERTIES=%BASE_DIR%\.mvn\wrapper\maven-wrapper.properties

IF NOT EXIST "%WRAPPER_PROPERTIES%" (
  ECHO Missing %WRAPPER_PROPERTIES%
  EXIT /B 1
)

FOR /F "tokens=1,* delims==" %%A IN (%WRAPPER_PROPERTIES%) DO (
  IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

IF "%DISTRIBUTION_URL%"=="" (
  ECHO distributionUrl is not configured
  EXIT /B 1
)

SET DIST_DIR=%BASE_DIR%\.mvn\wrapper\dists
IF NOT EXIST "%DIST_DIR%" mkdir "%DIST_DIR%"

SET ZIP_PATH=%DIST_DIR%\apache-maven-3.9.9-bin.zip
IF NOT EXIST "%ZIP_PATH%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ZIP_PATH%'"
)

SET MAVEN_HOME=%DIST_DIR%\apache-maven-3.9.9
IF NOT EXIST "%MAVEN_HOME%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%DIST_DIR%' -Force"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
