#!/usr/bin/env bash

PRG="$0"
go_tasks=("mobileBackup" "task")

while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done

PRGDIR=$(dirname "$PRG")
command=$1
run_go="false"
if printf '%s\n' "${go_tasks[@]}" | grep -q "^$command$"; then
    run_go="true"
fi


if [ "$run_go" = "true" ]; then
  "$PRGDIR"/lib/the-source-box "$@"
else
  java --enable-preview -jar "$PRGDIR"/lib/sourcebox-fat.jar "$@"
fi
