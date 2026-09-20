#!/bin/bash
if [ $# -eq 0 ]; then
  echo "Usage:
   sas.sh [jvm_options] /path/to/war [--port=8080] [--path=/yourbase] [other_args]
   sas.sh [jvm_options] group_id:artifact_id:version [--engine=undertow/tomcat] [other_args]
   sas.sh [jvm_options] http://host.com/path/towar [other_args]"
  exit 1
fi

PRGDIR=$(dirname "$0")
export SAS_HOME=$(cd "$PRGDIR/../" >/dev/null; pwd)
if [ -z "$M2_REMOTE_REPO" ]; then
  export M2_REMOTE_REPO="https://maven.aliyun.com/nexus/content/groups/public"
fi
if [ -z "$M2_REPO" ]; then
  export M2_REPO="$HOME/.m2/repository"
fi
export scala_ver=3.9.0
export scala_lib_ver=3.9.0
export scalaxml_ver=2.5.0
export beangle_sas_ver=0.13.13
export beangle_commons_ver=6.3.7
export beangle_template_ver=0.2.14
export beangle_boot_ver=0.1.30
export slf4j_ver=2.0.19
export logback_ver=1.6.3
export freemarker_ver=2.3.35
export commons_compress_ver=1.28.0
export tomcat_ver=11.0.26
export undertow_ver=2.4.3.Final
export undertow_ee_ver=2.0.2.Final

# launch classpath
bootpath=""
# full command line to java
opts="$*"
# war file,may be groupid/file/url
warfile=""
#contextpath
app_name="ROOT"
#java options
options=""
#java args
args=""
classpath=""
sas_home="/tmp/sas"
engine="tomcat"
is_cygwin=0
sep=":"

os_kernel=$(uname | tr 'A-Z' 'a-z')
if [[ "$os_kernel" == *"cygwin"* || "$os_kernel" == *"mingw"* ]]; then
  is_cygwin=1
  sep=";"
fi

local_path(){
  group_id=$(echo "$1" | tr . /)
  if [ $is_cygwin -eq 0 ]; then
    echo "$M2_REPO/$group_id/$2/$3/$2-$3.jar"
  else
    echo "$(cygpath -w "$M2_REPO/$group_id/$2/$3/$2-$3.jar")"
  fi
}

# download groupId artifactId version
# add append bootpath
download(){
  group_id=$(echo "$1" | tr . /)
  URL="$M2_REMOTE_REPO/$group_id/$2/$3/$2-$3.jar"
  artifact_name="$2-$3.jar"
  local_file="$M2_REPO/$group_id/$2/$3/$2-$3.jar"
  bootpath="$bootpath:$local_file"

  if [ -f "$local_file" ] && [ "$(head -c 2 "$local_file")" != "PK" ]; then
    rm -f "$local_file"
  fi

  if [ ! -f "$local_file" ]; then
    echo "fetching $URL"
    if curl -fsSL -o "$artifact_name" "$URL" 2>/dev/null && [ "$(head -c 2 "$artifact_name")" = "PK" ]; then
      mkdir -p "$M2_REPO/$group_id/$2/$3"
      mv "$artifact_name" "$local_file"
    else
      rm -f "$artifact_name"
      echo "$URL not exists,installation aborted."
      exit 1
    fi
  fi
}

# extract_arg "--path = /tmp"
extract_arg_value() {
  local input="$1"
  # 匹配 = 后的内容（去掉前面所有字符）
  local temp=${input#*=}
  # 去首尾空格
  echo "$temp" | xargs  # 去首尾空格
}

#find warfile/content_path in all opts
parse_args(){
  for arg in $opts
  do
    if [ "$arg" = "${arg#"-"}" ]; then
      warfile="$arg"
    elif [[ "$arg" == --path* ]] ; then
      app_name=$(extract_arg_value "$arg")
      app_name=$(echo "$app_name" | tr '/' '#')
      app_name=${app_name#"#"}
    elif [[ "$arg" == --engine* ]] ; then
      engine=$(extract_arg_value "$arg")
    fi
  done

  # try to find warfile arg
  if [ -z "$warfile" ]; then
    echo "Cannot find jar file in args,launch was aborted."
    exit
  fi

  #get options and args of java program,(format is options warfile args)
  options="${opts%%$warfile*}"
  args="${opts#*$warfile}"
}

parse_args

echo "Fetching bootstrap dependency"
download org.scala-lang scala3-library_3 $scala_ver
download org.scala-lang scala-library $scala_lib_ver
download org.beangle.commons beangle-commons $beangle_commons_ver
download org.apache.commons commons-compress $commons_compress_ver
download org.beangle.boot beangle-boot $beangle_boot_ver
download org.slf4j slf4j-api $slf4j_ver
download ch.qos.logback logback-core $logback_ver
download ch.qos.logback logback-classic $logback_ver

download org.beangle.sas beangle-sas-engine $beangle_sas_ver
download org.apache.tomcat.embed tomcat-embed-core $tomcat_ver
download org.apache.tomcat.embed tomcat-embed-websocket $tomcat_ver

download io.undertow undertow-core $undertow_ver
download io.undertow.ee undertow-servlet $undertow_ee_ver
download io.undertow.ee undertow-websockets $undertow_ee_ver
download org.jboss.logging jboss-logging 3.6.3.Final
download org.jboss.threads jboss-threads 3.9.2
download org.jboss.xnio xnio-api 3.8.16.Final
download org.jboss.xnio xnio-nio 3.8.16.Final
download jakarta.annotation jakarta.annotation-api 2.1.1
download jakarta.servlet jakarta.servlet-api 6.1.0
download jakarta.websocket jakarta.websocket-api 2.2.0
download jakarta.websocket jakarta.websocket-client-api 2.2.0
download org.wildfly.client wildfly-client-config 1.0.1.Final
download org.wildfly.common wildfly-common 2.0.1
download io.smallrye.common smallrye-common-annotation 2.14.0
download io.smallrye.common smallrye-common-constraint 2.12.0
download io.smallrye.common smallrye-common-cpu 2.14.0
download io.smallrye.common smallrye-common-expression 2.4.0
download io.smallrye.common smallrye-common-function 2.14.0
download io.smallrye.common smallrye-common-net 2.12.0
download io.smallrye.common smallrye-common-os 2.4.0
download io.smallrye.common smallrye-common-ref 2.4.0

bootpath="${bootpath:1}" #omit head :

#destfile is resolved absolute file path.
echo Resolving $warfile
destfile=$(java -cp "$bootpath" org.beangle.boot.dependency.AppResolver $warfile --remote=$M2_REMOTE_REPO --local=$M2_REPO --quiet --preferwar)
if [ $? -ne 0  ]; then
  echo "Cannot resolve $warfile, Launching aborted."
  exit
fi

doc_base="$sas_home/webapps/$app_name"
rm -rf $doc_base
mkdir -p $doc_base
unzip $destfile -d $doc_base > /dev/null 2>&1

if [ $? -ne 0 ]; then
  echo "unzip failed: $destfile -d $doc_base" >&2
  exit 1
fi

echo "Detecting Classpath and Main-Class"
bootinfo=$(java -cp "$bootpath" org.beangle.boot.launcher.Classpath $doc_base --local=$M2_REPO)

if [ $? = 0 ]; then
  if [ "$engine" = "tomcat" ]; then
    mainclass="org.beangle.sas.engine.tomcat.Bootstrap"
    classpath="${bootinfo#*@}"
    classpath=$classpath${sep}$(local_path org.apache.tomcat.embed tomcat-embed-core $tomcat_ver)
    classpath=$classpath${sep}$(local_path org.apache.tomcat.embed tomcat-embed-websocket $tomcat_ver)
    classpath=$classpath${sep}$(local_path org.beangle.sas beangle-sas-engine $beangle_sas_ver)
    export CLASSPATH="$classpath"
    java $options "$mainclass" --base=$sas_home $args
  elif [ "$engine" = "undertow" ]; then
    mainclass="org.beangle.sas.engine.undertow.Bootstrap"
    classpath="${bootinfo#*@}"
    classpath=$classpath${sep}$(local_path io.undertow undertow-core $undertow_ver)
    classpath=$classpath${sep}$(local_path io.undertow.ee undertow-servlet $undertow_ee_ver)
    classpath=$classpath${sep}$(local_path io.undertow.ee undertow-websockets $undertow_ee_ver)
    classpath=$classpath${sep}$(local_path org.jboss.logging jboss-logging 3.6.3.Final)
    classpath=$classpath${sep}$(local_path org.jboss.threads jboss-threads 3.9.2)
    classpath=$classpath${sep}$(local_path org.jboss.xnio xnio-api 3.8.16.Final)
    classpath=$classpath${sep}$(local_path org.jboss.xnio xnio-nio 3.8.16.Final)
    classpath=$classpath${sep}$(local_path jakarta.annotation jakarta.annotation-api 2.1.1)
    classpath=$classpath${sep}$(local_path jakarta.servlet jakarta.servlet-api 6.1.0)
    classpath=$classpath${sep}$(local_path jakarta.websocket jakarta.websocket-api 2.2.0)
    classpath=$classpath${sep}$(local_path jakarta.websocket jakarta.websocket-client-api 2.2.0)
    classpath=$classpath${sep}$(local_path org.wildfly.client wildfly-client-config 1.0.1.Final)
    classpath=$classpath${sep}$(local_path org.wildfly.common wildfly-common 2.0.1)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-annotation 2.14.0)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-constraint 2.12.0)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-cpu 2.14.0)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-expression 2.4.0)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-function 2.14.0)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-net 2.12.0)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-os 2.4.0)
    classpath=$classpath${sep}$(local_path io.smallrye.common smallrye-common-ref 2.4.0)

    classpath=$classpath${sep}$(local_path org.beangle.sas beangle-sas-engine $beangle_sas_ver)
    export CLASSPATH="$classpath"
    java $options "$mainclass" --base=$sas_home $args
  else
    echo "unknown engine $engine,launch failed."
  fi
else
   echo "launch failed."
fi
