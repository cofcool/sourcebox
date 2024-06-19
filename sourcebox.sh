#!/usr/bin/env bash

PRG="$0"

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

NATIVE="false"
COMMAND=$1
case $COMMAND in
  --native)
    NATIVE="true"
    shift
    ;;
  *)
    ;;
esac


if [ "$NATIVE" = "true" ]; then
  "$PRGDIR"/sourcebox -Dwebroot.dir=$PRGDIR/webroot "$@"
else
  java --enable-preview -jar "$PRGDIR"/lib/sourcebox-fat.jar "$@"
fi
