#!/bin/bash

if [ $# -eq 0 ]; then
  echo "Usage:
   launch.sh [jvm_options] /path/to/jar [args]
   launch.sh [jvm_options] group_id:artifact_id:jar|war:version [args]
   launch.sh [jvm_options] http://host.com/path/tojar [args]"
  exit 1
fi

# extra classpath_extra,it will be joined in Classpath program output
export_extra_classpath() {
  ## try extra classpath in options(--cp, -classpath,-class-path)
  classpath_prefix="-cp"
  classpath_extra=$(echo "$*" | sed 's/^.*-cp \(\S*\) .*$/\1/')
  if [ "$*" = "$classpath_extra" ]; then
    classpath_prefix="-classpath"
    classpath_extra=$(echo "$*" | sed 's/^.*-classpath \(\S*\) .*$/\1/')
  fi
  if [ "$*" = "$classpath_extra" ]; then
    classpath_prefix="--class-path"
    classpath_extra=$(echo "$*" | sed 's/^.*--class-path \(\S*\) .*$/\1/')
  fi

  if [ "$*" != "$classpath_extra" ]; then
    classpath_str="$classpath_prefix $classpath_extra"
    export classpath_extra
    opts="${opts#*"$classpath_str"}"
  fi
}

#find jarfile in opts
detect_jarfile(){
  for arg in $opts
  do
    if [ "$arg" = "${arg#"-"}" ]; then
      jarfile="$arg"
      break;
    fi
  done

  # try to find jar file
  if [ -z "$jarfile" ]; then
    echo "Cannot find jar file in args,launch was aborted."
    exit
  fi
}

# full command line to java
opts="$*"
# jar file
jarfile=""

export_extra_classpath "$opts"
detect_jarfile
#get options and args of java program
args="${opts#*$jarfile}" #parts after jarfile
options="${opts%%$jarfile*}" #parts before jarfile

source ./resolve.sh $jarfile
java $options $MAIN_CLASS $args
