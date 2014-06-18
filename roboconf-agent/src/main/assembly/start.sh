#######################################################################################
#
# There are three ways of launching the agent locally.
#
#######################################################################################
#
# It can be started locally with 6 mandatory parameters (in this order).
# - The application name
# - The root instance name (in the model)
# - The IP address of the messaging server (can end with :port-number)
# - The user name for the messaging server
# - The password for the messaging server
# - The IP address of the agent (useful for when there are several network interfaces)
#
# The IP address is the one that will be sent to other agents.
#
########################################################################################
#
# It can also be started with 2 parameters (in this order).
# - The string "platform".
# - The name of the IaaS it is running on.
#
# For the moment, only "Azure", "EC2" and "OpenStack" are supported in this mode.
# Information will be retrieved from user data passed by the DM to the IaaS.
#
########################################################################################
#
# Eventually, it can be started with 1 parameter.
# - The absolute path to a properties file that contain agent data.
#
# Information will be loaded from this file.
#
########################################################################################

export PATH=/usr/local/bin:$PATH
java -Djava.util.logging.config.file=./logging.properties -cp "lib/*" net.roboconf.agent.internal.Main $*
