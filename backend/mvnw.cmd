@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script for Windows
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%" == "1" @ECHO ON
@SETLOCAL

@SET ERROR_CODE=0

@SET MAVEN_DIR=%~dp0maven\apache-maven-3.9.9\bin\mvn.cmd
@IF EXIST "%MAVEN_DIR%" (
  CALL "%MAVEN_DIR%" %*
  @IF ERRORLEVEL 1 GOTO error
  @GOTO end
)

CALL mvn %*
@IF ERRORLEVEL 1 GOTO error
@GOTO end

:error
@SET ERROR_CODE=1

:end
@EXIT /B %ERROR_CODE%
