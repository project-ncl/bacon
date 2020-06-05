#!/bin/bash

# TODO: do it from Java

if [ -z "$1" ]
then
  echo "Repository zip not specified!"
  exit 1
fi

if [ -z "$2" ]
then
  echo "NVR list file path not specified!"
  exit 1
fi

if [ -z "$3" ]
then
  echo "Koji hub url not provided"
else
  KOJI_URL=$3
fi

REPOSITORY_ZIP=$1
NVR_LIST_NAME=$2
BUILD_DIR=nvr-list
KBF_URL=https://repo1.maven.org/maven2/com/redhat/red/build/koji-build-finder/1.1.0/koji-build-finder-1.1.0-jar-with-dependencies.jar
KBF_JAR=project-source-finder.jar

echo "Generate NVR list from $REPOSITORY_ZIP to $NVR_LIST_NAME"

if [ -d $BUILD_DIR ]
then
  rm -R $BUILD_DIR/*
else
  mkdir $BUILD_DIR
fi

# See also <https://docs.engineering.redhat.com/display/JP/Koji+Build+Finder>
if [ ! -e $BUILD_DIR/$KBF_JAR ]
then
   curl -L $KBF_URL -o $BUILD_DIR/$KBF_JAR
fi

cd $BUILD_DIR
java -jar project-source-finder.jar --koji-hub-url ${KOJI_URL} ${REPOSITORY_ZIP}
grep nvr builds.json | grep "redhat" | sort | sed 's/      "nvr" : "//;s/",//' | uniq > $2
