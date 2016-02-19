#!/bin/bash

# Get the Java version.
# Java 1.5 will give 15.
# Java 1.6 will give 16.
# Java 1.7 will give 17.
WHOLE_VER=`java -version 2>&1`
VER=`java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q'`

# We build for several JDKs on Travis.
# OpenJDK 7 => deploy artifacts on a Maven repository.

# If the version is OpenJDK 1.7, then perform the following actions.
# 1. Copy Maven settings on the VM.
# 2. Upload artifacts to Sonatype.
# 3. Use -q option to only display Maven errors and warnings.
# 4. Use --settings to force the usage of our "settings.xml" file.
# 5. Enable the profile that generates the javadoc and the sources archives.

if [[ $VER == "17" ]] && [[ $WHOLE_VER == *OpenJDK* ]]; then
	wget http://roboconf.net/resources/build/settings.xml
	mvn clean deploy -DskipTests=true -q --settings settings.xml -P jdoc-and-sources
elif [[ $VER == "18" ]]; then
	mvn javadoc:javadoc -q -Droboconf.javadoc.check -DskipTests=true
else
	echo "No action to undertake (not OpenJDK 7)."
fi
