#!/bin/bash

set -e

# Harmony BlockChainl
# Check if the jar has been built.
if [ ! -e target/harmony-1.0-SNAPSHOT-jar-with-dependencies.jar ]; then
  echo "Compiling blockchain project to a JAR"
  mvn package -DskipTests
fi

java -jar target/harmony-1.0-SNAPSHOT-jar-with-dependencies.jar "$@"