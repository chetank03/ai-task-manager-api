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
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
# ----------------------------------------------------------------------------

# If JAVA_HOME is set, use it; otherwise use java from PATH
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Determine the script directory
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-}"
if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
    # Find the project base dir
    wdir="$PWD"
    while [ "$wdir" != '/' ]; do
        if [ -f "$wdir/pom.xml" ]; then
            MAVEN_PROJECTBASEDIR="$wdir"
            break
        fi
        wdir=$(dirname "$wdir")
    done
    if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
        MAVEN_PROJECTBASEDIR="$PWD"
    fi
fi

MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Read the wrapper URL from properties
WRAPPER_URL=""
if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    WRAPPER_URL=$(grep "wrapperUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2-)
fi

# Download the wrapper jar if it doesn't exist
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    if command -v curl > /dev/null 2>&1; then
        curl -fsL "$WRAPPER_URL" -o "$MAVEN_WRAPPER_JAR" || true
    elif command -v wget > /dev/null 2>&1; then
        wget -q "$WRAPPER_URL" -O "$MAVEN_WRAPPER_JAR" || true
    fi
fi

# Run Maven via the wrapper
exec "$JAVACMD" \
    -classpath "$MAVEN_WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
