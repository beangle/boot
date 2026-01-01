@echo off
REM Windows batch script to resolve jar file and extract Main-Class and classpath
REM Usage: call resolve.bat jar_path_or_maven_coordinates
REM Note: Use 'call resolve.bat' (not just 'resolve.bat') to keep environment variables
REM Example: call resolve.bat C:\path\to\app.jar
REM          call resolve.bat com.example:myapp:jar:1.0.0

setlocal enabledelayedexpansion

if "%~1"=="" (
  echo Usage:
  echo   resolve.bat C:\path\to\jar
  echo   resolve.bat group_id:artifact_id:jar^|war:version
  echo   resolve.bat http://host.com/path/to/jar
  exit /b 1
)

if "%M2_REMOTE_REPO%"=="" set M2_REMOTE_REPO=https://maven.aliyun.com/repository/public
if "%M2_REPO%"=="" set M2_REPO=%USERPROFILE%\.m2\repository

set jar=%~1
set scala_ver=3.3.7
set scala_lib_ver=2.13.16
set beangle_commons_ver=5.6.33
set commons_compress_ver=1.28.0
set boot_ver=0.1.22

set bootpath=

call :download org.scala-lang scala3-library_3 %scala_ver%
call :download org.scala-lang scala-library %scala_lib_ver%
call :download org.beangle.commons beangle-commons %beangle_commons_ver%
call :download org.apache.commons commons-compress %commons_compress_ver%
call :download org.beangle.boot beangle-boot %boot_ver%

REM Remove leading semicolon (Windows uses semicolon as classpath separator)
set bootpath=!bootpath:~1!

REM avoid dirty classpath and echo log (add small delay)
set "CLASSPATH="
echo Resolving !jar!
java -cp "!bootpath!" org.beangle.boot.dependency.AppResolver "!jar!" --remote=!M2_REMOTE_REPO! --local=!M2_REPO! --quiet > temp_resolve.txt 2>&1
set resolve_error=!errorlevel!
set /p resolveResult=<temp_resolve.txt
del temp_resolve.txt

if !resolve_error! neq 0 (
  echo !resolveResult!
  echo Cannot resolve !jar!, resolving aborted.
  exit /b 1
) else (
  set jar=!resolveResult!
)

REM avoid dirty classpath
set "CLASSPATH="
set "info="
echo Detecting Classpath and Main-Class
java -cp "!bootpath!" org.beangle.boot.launcher.Classpath "!jar!" --local=!M2_REPO! > temp_info.txt 2>&1
set info_error=!errorlevel!
for /f "tokens=* delims=" %%a in (temp_info.txt) do (
  set "info=!info!%%a"
)
del temp_info.txt

if !info_error! equ 0 (
  for /f "tokens=1* delims=@" %%a in ("!info!") do (
    set mainclass=%%a
    set classpath=%%b
  )

  if "!mainclass!"=="none" (
    echo Cannot find Main-Class in MANIFEST.MF of !jar!
    endlocal
    exit /b 1
  ) else (
    REM Save variables to temp file before endlocal
    echo CLASSPATH=!classpath!> resolve_vars.tmp
    echo MAIN_CLASS=!mainclass!>> resolve_vars.tmp
    endlocal

    REM Restore variables to parent environment
    for /f "tokens=1* delims==" %%a in (resolve_vars.tmp) do (
      set "%%a=%%b"
    )
    del resolve_vars.tmp
  )
) else (
  echo !info!
  endlocal
  exit /b 1
)

goto :eof

:download
set group_id=%~1
set artifact=%~2
set version=%~3

REM Replace dots with slashes in group_id
set "group_path=%group_id:.=/%"
set "group_path_win=%group_id:.=\%"

set URL=!M2_REMOTE_REPO!/!group_path!/!artifact!/!version!/!artifact!-!version!.jar
set artifact_name=!artifact!-!version!.jar
set local_file=!M2_REPO!\!group_path_win!\!artifact!\!version!\!artifact!-!version!.jar
set bootpath=!bootpath!;!local_file!

if not exist "!local_file!" (
  REM Create directory structure first
  set "local_dir=!M2_REPO!\!group_path_win!\!artifact!\!version!"
  if not exist "!local_dir!" mkdir "!local_dir!"

  echo fetching !URL!

  REM Download using PowerShell (directly try download, similar to wget behavior)
  powershell -NoProfile -Command "$ErrorActionPreference='Stop'; try { Invoke-WebRequest -Uri '!URL!' -OutFile '!artifact_name!' -UseBasicParsing; exit 0 } catch { Write-Host $_.Exception.Message; exit 1 }"

  if errorlevel 1 (
    echo Failed to download !URL!, file may not exist.
    exit /b 1
  )

  if not exist "!artifact_name!" (
    echo Download failed: "!artifact_name!" not found after download attempt.
    exit /b 1
  )

  move "!artifact_name!" "!local_dir!" >nul 2>&1
  if errorlevel 1 (
    echo Failed to move "!artifact_name!" to "!local_dir!"
    exit /b 1
  )
)

exit /b 0

