#!/bin/bash
cd "$(dirname $BASH_SOURCE)"
cd ..
java -jar ${artifactId}.jar
