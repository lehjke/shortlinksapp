@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required JAVA_HOME or PATH to be set to JDK installation.
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set ERROR_CODE=0

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%"=="" goto OkJHome

for %%i in (java.exe) do set JAVA_EXE=%%~$PATH:i
if not "%JAVA_EXE%"=="" goto init

echo.
echo Error: JAVA_HOME not found and java.exe not in PATH.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto error

:OkJHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo Warning: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = "%JAVA_HOME%"
echo Falling back to java.exe from PATH.
for %%i in (java.exe) do set JAVA_EXE=%%~$PATH:i
if not "%JAVA_EXE%"=="" goto init
echo Error: java.exe not found. Please fix JAVA_HOME or update PATH.
goto error

:init
set WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set MAVEN_CMD_LINE_ARGS=%*

"%JAVA_EXE%" ^
  -Dmaven.multiModuleProjectDirectory=%CD% ^
  %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CMD_LINE_ARGS%

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
endlocal & set ERROR_CODE=%ERROR_CODE%

if not "%MAVEN_BATCH_PAUSE%"=="" pause

if "%MAVEN_SKIP_RC%"=="" goto skipRcPre
goto skipRcPost
:skipRcPre
if exist "%USERPROFILE%\mavenrc_pre.bat" call "%USERPROFILE%\mavenrc_pre.bat"
if exist "%USERPROFILE%\mavenrc_pre.cmd" call "%USERPROFILE%\mavenrc_pre.cmd"
:skipRcPost
if exist "%USERPROFILE%\mavenrc_post.bat" call "%USERPROFILE%\mavenrc_post.bat"
if exist "%USERPROFILE%\mavenrc_post.cmd" call "%USERPROFILE%\mavenrc_post.cmd"

exit /b %ERROR_CODE%
