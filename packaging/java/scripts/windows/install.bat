@REM
@REM ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
@REM
@REM Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
@REM
@REM NOTICE: All information contained herein is, and remains
@REM the property of ThingsBoard, Inc. and its suppliers,
@REM if any.  The intellectual and technical concepts contained
@REM herein are proprietary to ThingsBoard, Inc.
@REM and its suppliers and may be covered by U.S. and Foreign Patents,
@REM patents in process, and are protected by trade secret or copyright law.
@REM
@REM Dissemination of this information or reproduction of this material is strictly forbidden
@REM unless prior written permission is obtained from COMPANY.
@REM
@REM Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
@REM managers or contractors who have executed Confidentiality and Non-disclosure agreements
@REM explicitly covering such access.
@REM
@REM The copyright notice above does not evidence any actual or intended publication
@REM or disclosure  of  this source code, which includes
@REM information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
@REM ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
@REM OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
@REM THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
@REM AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
@REM THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
@REM DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
@REM OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
@REM

@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Detecting Java version installed.
:CHECK_JAVA
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "jver=%%j%%k"
@ECHO CurrentVersion %jver%

if %jver% NEQ 170 GOTO JAVA_NOT_INSTALLED

:JAVA_INSTALLED

@ECHO Java 17 found!
@ECHO Installing thingsboard ...

SET loadDemo=false

if "%1" == "--loadDemo" (
    SET loadDemo=true
)

SET BASE=%~dp0
SET LOADER_PATH=%BASE%\conf,%BASE%\extensions
SET SQL_DATA_FOLDER=%BASE%\data\sql
SET jarfile=%BASE%\lib\${pkg.name}.jar
SET installDir=%BASE%\data

PUSHD "%BASE%\conf"

java -cp "%jarfile%" -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir="%installDir%"^
                    -Dinstall.load_demo=%loadDemo%^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=false^
                    -Dlogging.config="%BASE%\install\logback.xml"^
                    org.springframework.boot.loader.launch.PropertiesLauncher

if errorlevel 1 (
   @echo ThingsBoard installation failed!
   POPD
   exit /b %errorlevel%
)
POPD

"%BASE%"thingsboard.exe install

@ECHO ThingsBoard installed successfully!

GOTO END

:JAVA_NOT_INSTALLED
@ECHO Java 17 is not installed. Only Java 17 is supported
@ECHO Please go to https://adoptopenjdk.net/index.html and install Java 17. Then retry installation.
PAUSE
GOTO END

:END
