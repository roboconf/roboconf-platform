#!/bin/bash

# Get the Java version.
# Java 1.5 will give 15.
# Java 1.6 will give 16.
# Java 1.7 will give 17.
WHOLE_VER=`java -version 2>&1`
VER=`java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q'`

# We build for several JDKs on Travis.
# OpenJDK 7 => Run Cobertura and Coveralls.

if [[ $VER == "17" ]] && [[ $WHOLE_VER == *OpenJDK* ]]; then
	mvn clean cobertura:cobertura install coveralls:report -q
elif [[ $VER == "18" ]]; then
	mvn clean install javadoc:javadoc -q -Droboconf.javadoc.check
fi
