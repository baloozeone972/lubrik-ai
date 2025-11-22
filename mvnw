#!/bin/sh
# Maven Wrapper Script
# Downloads and runs Maven if not present

set -e

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

# Download wrapper if needed
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    if command -v curl > /dev/null; then
        curl -sL -o "$MAVEN_WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    elif command -v wget > /dev/null; then
        wget -q -O "$MAVEN_WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    fi
fi

# Run Maven
exec java -jar "$MAVEN_WRAPPER_JAR" "$@"
