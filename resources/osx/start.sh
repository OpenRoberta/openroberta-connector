#!/bin/bash

JAVA_DIR=
if ! hash java 2>/dev/null; then
  JAVA_DIR=/Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/
fi

cd "$(dirname $BASH_SOURCE)"
cd ..
"$JAVA_DIR"java -jar ${artifactId}.jar
