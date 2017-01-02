This directory contains templates for configuration files.
They are read by the agent when its configuration changes.
All these files should have the ".cfg.tpl" extension.

The following agent's parameters can be injected in these templates.

- <domain>: the agent's domain.
- <application-name>: the name of the application this agent is associated with.
- <scoped-instance-path>: the path of the scoped instance this agent is associated with.
- <ip-address>: the agent's IP address (retrieved by the agent itself or the one forced in its configuration).

Notice that the "net.roboconf.agent.configuration.cfg" is excluded from this mechanism.
That would result in an infinite reconfiguration loop.
