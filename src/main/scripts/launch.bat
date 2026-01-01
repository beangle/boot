@echo off
REM Windows batch script to launch Java application with jar resolution
REM Usage: launch.bat [jvm_options] jar_path_or_maven_coordinates [args]
REM Example: launch.bat -Xmx512m C:\path\to\app.jar arg1 arg2
REM          launch.bat -cp extra.jar com.example:myapp:jar:1.0.0 arg1

setlocal enabledelayedexpansion

if "%~1"=="" (
  echo Usage:
  echo   launch.bat [jvm_options] C:\path\to\jar [args]
  echo   launch.bat [jvm_options] group_id:artifact_id:jar^|war:version [args]
  echo   launch.bat [jvm_options] http://host.com/path/to/jar [args]
  exit /b 1
)

REM Full command line options
set "opts=%*"
set "jarfile="
set "classpath_extra="
set "options="
set "args="

REM Get options (before jarfile) and args (after jarfile)
call :split_args

if "!jarfile!"=="" (
  echo Cannot find jar file in args, launch was aborted.
  exit /b 1
)

REM Call resolve.bat to get MAIN_CLASS and CLASSPATH
call resolve.bat "!jarfile!"
if errorlevel 1 (
  echo Failed to resolve jar file: !jarfile!
  exit /b 1
)

java !options! %MAIN_CLASS% !args!
endlocal
goto :eof

REM Split opts into classpath_extra/jarfile/options/args
:split_args
set "jarfile_found=0"
set "extract_cp=0"

:split_str
for /f "tokens=1,* delims= " %%a in ("!opts!") do (
  set arg=%%a
  set "opts=%%b"

  if !extract_cp! equ 1 (
    REM Extract classpath value from -cp (don't add to options)
    set "classpath_extra=!arg!"
    set "extract_cp=0"
  ) else (
    REM Check if argument starts with -
    set "first_char=!arg:~0,1!"
    set "is_jar_args=0"
    if not "!first_char!"=="-" (
      if !jarfile_found! equ 0 (
        set "is_jar_args=1"
      )
    )
    if !is_jar_args! equ 1 (
      set "jarfile=!arg!"
      set "jarfile_found=1"
    ) else (
      REM Check if this is -cp option (don't add to options, extract its value instead)
      if "!arg!"=="-cp" (
        set "extract_cp=1"
      ) else (
        REM Add to options or args based on jarfile_found flag
        REM All other arguments (including -Xms512m, -Xmx1024m, etc.) are added normally
        if !jarfile_found! equ 0 (
          REM Before jarfile = options
          if "!options!"=="" (
            set "options=!arg!"
          ) else (
            set "options=!options! %%a"
          )
        ) else (
          REM After jarfile = args
          if "!args!"=="" (
            set "args=%%a"
          ) else (
            set "args=!args! %%a"
          )
        )
      )
    )
  )
)
if not "!opts!"=="" goto :split_str

exit /b 0
