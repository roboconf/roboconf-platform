#!/bin/bash

# Get the Java version.
# Java 1.5 will give 15.
# Java 1.6 will give 16.
# Java 1.7 will give 17.
WHOLE_VER=`java -version 2>&1`
VER=`java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q'`

# We build for several JDKs on Travis.
# OpenJDK 7 => Compile, run unit and integration tests. Also run Cobertura and Coveralls.
# Others => Compile and run unit tests, but skip integration tests.

if [[ $VER == "17" ]] && [[ $WHOLE_VER == *OpenJDK* ]]; then
	mvn clean cobertura:cobertura install coveralls:report -P run-integration-tests -q
else
	mvn clean install -q
fi
