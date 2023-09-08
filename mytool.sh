#!/usr/bin/env bash

PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`

NATIVE="true"
COMMAND=$1
case $COMMAND in
  --jar)
    NATIVE="false"
    shift
    ;;
  *)
    ;;
esac


if [ "$NATIVE" = "true" ]; then
	echo "Start native toolbox"
	dir=`pwd`
	cd $PRGDIR
  $PRGDIR/my-toolbox $@
	cd $dir
else
  java -jar $PRGDIR/lib/my-toolbox-fat.jar $@
fi
