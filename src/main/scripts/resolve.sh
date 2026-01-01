#!/bin/bash

# download groupId artifactId version
download(){
  group_id=`echo "$1" | tr . /`
  URL="$M2_REMOTE_REPO/$group_id/$2/$3/$2-$3.jar"
  artifact_name="$2-$3.jar"
  local_file="$M2_REPO/$group_id/$2/$3/$2-$3.jar"
  bootpath=$bootpath":"$local_file

  if [ ! -f $local_file ]; then
    if curl -O $URL 2>/dev/null; then
      echo "fetching $URL"
    else
      echo "$URL not exists,installation aborted."
      exit 1
    fi
    mkdir -p "$M2_REPO/$group_id/$2/$3"
    mv $artifact_name $local_file
  fi
}

if [ $# -eq 0 ]; then
  echo "Usage:
   resolve.sh /path/to/jar
   resolve.sh group_id:artifact_id:jar|war:version
   resolve.sh http://host.com/path/to/jar"
  exit 1
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
scala_ver="3.3.7"
scala_lib_ver="2.13.16"
beangle_commons_ver="5.6.33"
commons_compress_ver="1.28.0"
boot_ver="0.1.22"

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
  exit 1;
else
  jar="$resolveResult"
fi

info=$(java -cp "$bootpath" org.beangle.boot.launcher.Classpath $jar --local=$M2_REPO)
if [ $? = 0 ]; then
  mainclass="${info%@*}"
  classpath="${info#*@}"
  if [ "none" = "$mainclass" ]; then
    echo "Cannot find Main-Class in MANIFEST.MF of $jar"
    exit 1;
  else
    export CLASSPATH="$classpath"
    export MAIN_CLASS="$mainclass"
  fi
else
   echo $info
   exit 1;
fi
