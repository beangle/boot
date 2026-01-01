#!/bin/bash

#find jarfile classpath_extra,options,args in all options
split_args(){
  jarfile_found=0
  extract_cp=0
  IFS=' ' read -r -a args_array <<< "$opts"
  for arg in "${args_array[@]}"; do
    if [ $extract_cp -eq 1 ]; then
      classpath_extra="$arg"
      extract_cp=0
    else
      if [[ "$arg" != -* && $jarfile_found -eq 0 ]]; then
        jarfile="$arg"
        jarfile_found=1
      else
        if [ "$arg" == "-cp" ]; then
          extract_cp=1
        else
          if [ $jarfile_found -eq 0 ]; then
            if [ "$options" == "" ]; then
              options="$arg"
            else
              options="$options $arg"
            fi
          else
            if [ "$args" == "" ]; then
              args="$arg"
            else
              args="$args $arg"
            fi
          fi
        fi
      fi
    fi
  done
}

if [ $# -eq 0 ]; then
  echo "Usage:
   launch.sh [jvm_options] /path/to/jar [args]
   launch.sh [jvm_options] group_id:artifact_id:jar|war:version [args]
   launch.sh [jvm_options] http://host.com/path/tojar [args]"
  exit 1
fi

opts="$*"
jarfile=""
classpath_extra=""
options=""
args=""
split_args

if [ -z "$jarfile" ]; then
  echo "Cannot find jar file in args,launch was aborted."
  exit
fi
if [ -n "$classpath_extra" ]; then
  export classpath_extra
fi

source ./resolve.sh $jarfile
#echo jarfile is $jarfile
#echo classpath_extra is $classpath_extra
#echo options is $options
#echo args is $args
#echo CLASSPATH is $CLASSPATH
#echo MAIN_CLASS is $MAIN_CLASS

java $options $MAIN_CLASS $args
