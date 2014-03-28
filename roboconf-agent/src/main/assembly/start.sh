# Can be started locally with 4 params:
# applicationName, rootInstanceName (= machine name), ipMessagingServer, ipAddress
java -Djava.util.logging.config.file=./logging.properties -cp "lib/*" net.roboconf.agent.internal.Main $*
