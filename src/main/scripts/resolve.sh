#!/bin/bash

_resolve_fail() {
  local code="${1:-1}"
  if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
    return "$code"
  else
    exit "$code"
  fi
}

# download groupId artifactId version
download(){
  group_id=`echo "$1" | tr . /`
  URL="$M2_REMOTE_REPO/$group_id/$2/$3/$2-$3.jar"
  artifact_name="$2-$3.jar"
  local_file="$M2_REPO/$group_id/$2/$3/$2-$3.jar"
  bootpath=$bootpath":"$local_file

  if [ ! -f $local_file ]; then
    if curl -O $URL 2>/dev/null; then
      echo "Fetching $URL"
    else
      rm -f "$remote_filename"
      echo "Downloading Error $URL,installation aborted."
      _resolve_fail 1
    fi
    mkdir -p "$M2_REPO/$group_id/$2/$3"
    mv $artifact_name $local_file
  fi
}

if [ $# -eq 0 ]; then
  echo "Usage:
   resolve.sh /path/to/jar [classpath_file]
   resolve.sh group_id:artifact_id:jar|war:version [classpath_file]
   resolve.sh http://host.com/path/to/jar [classpath_file]

  After sourcing, launch with:
   java -cp @\$CLASSPATH_FILE \$MAIN_CLASS [args]

  classpath_file defaults to ./classpath (absolute path in CLASSPATH_FILE)."
  _resolve_fail 1
fi

if [ -z "$M2_REMOTE_REPO" ]; then
  export M2_REMOTE_REPO="https://maven.aliyun.com/repository/public"
fi
if [ -z "$M2_REPO" ]; then
  export M2_REPO="$HOME/.m2/repository"
fi

# launch classpath
bootpath=""
jar="$1"
classpath_file_arg="$2"
scala_ver="3.3.7"
scala_lib_ver="2.13.16"
beangle_commons_ver="6.1.0"
commons_compress_ver="1.28.0"
boot_ver="0.1.27"

download org.scala-lang scala3-library_3 $scala_ver
download org.scala-lang scala-library $scala_lib_ver
download org.beangle.commons beangle-commons  $beangle_commons_ver
download org.apache.commons commons-compress $commons_compress_ver
download org.beangle.boot beangle-boot $boot_ver

bootpath="${bootpath:1}" #omit head :

resolveResult=$(java -cp "$bootpath" org.beangle.boot.dependency.AppResolver $jar --remote=$M2_REMOTE_REPO --local=$M2_REPO --quiet)
if [ $? -ne 0  ]; then
  echo $resolveResult
  echo "Cannot resolve $jar, resolving aborted."
  _resolve_fail 1;
else
  jar="$resolveResult"
fi

info=$(java -cp "$bootpath" org.beangle.boot.launcher.Classpath $jar --local=$M2_REPO)
if [ $? = 0 ]; then
  mainclass="${info%@*}"
  classpath="${info#*@}"
  if [ "none" = "$mainclass" ]; then
    echo "Cannot find Main-Class in MANIFEST.MF of $jar"
    _resolve_fail 1;
  else
    classpath_file="${classpath_file_arg:-classpath}"
    if [[ "$classpath_file" != /* ]]; then
      classpath_file="$(pwd)/$classpath_file"
    fi
    classpath_dir="$(dirname "$classpath_file")"
    if [ ! -d "$classpath_dir" ]; then
      echo "Classpath directory does not exist: $classpath_dir"
      _resolve_fail 1
    fi
    printf '%s' "$classpath" > "$classpath_file"
    export CLASSPATH_FILE="$classpath_file"
    export MAIN_CLASS="$mainclass"
  fi
else
   echo $info
   _resolve_fail 1;
fi
