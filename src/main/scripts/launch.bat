@echo off

if [ $# -eq 0 ]; then
  echo "Usage:
   launch.bat [jvm_options] /path/to/jar [args]
   launch.bat [jvm_options] group_id:artifact_id:jar|war:version [args]
   launch.bat [jvm_options] http://host.com/path/tojar [args]"
  exit 1
fi

IF "%M2_REMOTE_REPO%"=="" set M2_REMOTE_REPO="https://maven.aliyun.com/repository/public"

if "%$M2_REPO%"==""  set M2_REPO="$HOME/.m2/repository"

echo "under construction..."

echo "try to learn bat grammar"
