#!/bin/sh
if [ -z "$M2_REMOTE_REPO" ]; then
  export M2_REMOTE_REPO="https://maven.aliyun.com/nexus/content/groups/public"
fi
if [ -z "$M2_REPO" ]; then
  export M2_REPO="$HOME/.m2/repository"
fi

classpath=""
# download groupId artifactId version
download(){
  local group_id=`echo "$1" | tr . /`
  local URL="$M2_REMOTE_REPO/$group_id/$2/$3/$2-$3.$4"
  local artifact_name="$2-$3.$4"
  local local_file="$M2_REPO/$group_id/$2/$3/$2-$3.$4"
  classpath+=local_file

  if [ ! -f $local_file ]; then
    if wget --spider $URL 2>/dev/null; then
      echo "fetching $URL"
    else
      echo "$URL not exists,installation aborted."
      exit 1
    fi

    if command -v aria2c >/dev/null 2; then
      aria2c -x 16 $URL
    else
      wget $URL -O $artifact_name.part
      mv $artifact_name.part $artifact_name
    fi
    mkdir -p "$M2_REPO/$group_id/$2/$3"
    mv $artifact_name $local_file
  fi
}

export scala_ver=2.13.3
export scalaxml_ver=2.0.0-M1
export beangle_commons_ver=5.2.0
export beangle_template_ver=0.0.28
export slf4j_ver=2.0.0-alpha1
export logback_ver=1.3.0-alpha5
export commons_compress_ver=1.18
export boot_ver=0.0.1-SNAPSHOT

  download org.scala-lang scala-library $scala_ver jar
  download org.scala-lang scala-reflect $scala_ver jar
  download org.scala-lang.modules scala-xml_2.13 $scalaxml_ver jar
  download org.beangle.commons beangle-commons-core_2.13  $beangle_commons_ver jar
  download org.beangle.commons beangle-commons-file_2.13  $beangle_commons_ver jar
  download org.apache.commons commons-compress $commons_compress_ver jar
  download org.beangle.boot beangle-boot-launcher_2.13  $boot_ver jar
  download org.slf4j slf4j-api $slf4j_ver jar
  download ch.qos.logback logback-core $logback_ver jar
  download ch.qos.logback logback-classic $logback_ver jar
  download ch.qos.logback logback-access $logback_ver jar

echo $classpath
# java -cp "lib/*" org.beangle.boot.launcher.Launch "${@}"