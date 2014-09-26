# Roboconf :: Karaf Branding for the Agent

This Maven module aims at providing resources to brand Karaf.

Despite its JAR packaging, it does not contain any Java code.  
And given Sonatype requirements to publish artifacts on Maven Central, we must
provide a **-javadoc.jar**, even if it is empty or only contains a readme.

Hence the presence of this file.
