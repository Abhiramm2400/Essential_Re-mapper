#!/bin/bash
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
  java -jar gradle/wrapper/gradle-wrapper.jar "$@"
else
  gradle "$@"
fi
