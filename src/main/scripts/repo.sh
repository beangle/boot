#!/bin/bash

repo_fail() {
  local code="${1:-1}"
  if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
    return "$code"
  else
    exit "$code"
  fi
}

local_jar() {
  group_id=$(echo "$1" | tr . /)
  local file="$M2_REPO/$group_id/$2/$3/$2-$3.jar"
  if [ ! -f "$file" ]; then
    echo "Cannot find $file"
    repo_fail 1
  fi
  echo "$file"
}

if [ $# -eq 0 ]; then
  echo "Usage:
   repo.sh /path/to/jar [--local=local_base] [--source=source_base] [--quiet]

  Consolidate dependency jars from source repo into --local repo.
  --source defaults to M2_REPO ($HOME/.m2/repository)."
  repo_fail 1
fi

if [ -z "$M2_REPO" ]; then
  export M2_REPO="$HOME/.m2/repository"
fi

jar="$1"
shift
scala_ver="3.9.0"
beangle_commons_ver="6.3.7"
commons_compress_ver="1.28.0"
boot_ver="0.1.30"

bootpath="$(local_jar org.scala-lang scala3-library_3 $scala_ver)"
bootpath="$bootpath:$(local_jar org.scala-lang scala-library $scala_ver)"
bootpath="$bootpath:$(local_jar org.beangle.commons beangle-commons $beangle_commons_ver)"
bootpath="$bootpath:$(local_jar org.apache.commons commons-compress $commons_compress_ver)"
bootpath="$bootpath:$(local_jar org.beangle.boot beangle-boot $boot_ver)"

if ! result=$(java -cp "$bootpath" org.beangle.boot.launcher.Repo "$jar" --source="$M2_REPO" "$@"); then
  repo_fail 1
fi
echo "$result"
