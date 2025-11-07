#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

set -euo pipefail

if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_EXEC="$(command -v java || true)"
else
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVA_EXEC" ]; then
  echo "Error: JAVA_HOME is not defined correctly or java is not in PATH" >&2
  exit 1
fi

WRAPPER_JAR="$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="org.apache.maven.wrapper.MavenWrapperMain"

exec "$JAVA_EXEC" \
  -Dmaven.multiModuleProjectDirectory="$(pwd -P)" \
  ${MAVEN_OPTS:-} ${MAVEN_DEBUG_OPTS:-} \
  -classpath "$WRAPPER_JAR" \
  "$WRAPPER_LAUNCHER" "$@"
