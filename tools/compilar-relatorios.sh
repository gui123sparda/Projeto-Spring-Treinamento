#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./mvnw -q -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target/relatorios-classpath.txt
java_exec=java
if [ -n "${JAVA_HOME:-}" ]; then java_exec="$JAVA_HOME/bin/java"; fi
"$java_exec" -Djava.awt.headless=true --class-path "target/classes:$(cat target/relatorios-classpath.txt)" tools/CompilarRelatorios.java
