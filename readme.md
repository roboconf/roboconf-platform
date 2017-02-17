# Roboconf 
[![Build Status](http://travis-ci.org/roboconf/roboconf-platform.png?branch=master)](http://travis-ci.org/roboconf/roboconf-platform/builds)
[![Coverage Status](https://coveralls.io/repos/roboconf/roboconf-platform/badge.svg?branch=master&service=github)](https://coveralls.io/github/roboconf/roboconf-platform?branch=master)
[![License](https://img.shields.io/badge/license-Apache%20v2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Join us on Gitter.im](https://img.shields.io/badge/gitter-join%20chat-brightgreen.svg)](https://gitter.im/roboconf/roboconf)
[![Web site](https://img.shields.io/badge/website-roboconf.net-b23e4b.svg)](http://roboconf.net)

[![Snapshots](https://img.shields.io/badge/Snapshots%20on-Sonatype-orange.svg)](https://oss.sonatype.org/content/repositories/snapshots/net/roboconf/)
[![Maven Central](https://img.shields.io/badge/Releases%20on-Maven%20Central-yellow.svg)](http://repo1.maven.org/maven2/net/roboconf/)

This repository contains the source code for the Roboconf platform.  
This includes the manager, the agent, their default distributions, the tooling core and the Maven plugin.

What is Roboconf?
=================

Roboconf is both a platform and framework to manage elastic applications in the cloud.  
Elastic applications designate those whose deployment topology may vary over time (e.g. scaling up or down).
Roboconf manages deployments, probes, automatic reactions and reconfigurations. Beyond applications, Roboconf could also be defined as a « PaaS framework »: a solution to build PaaS (Platform as a Service). Most PaaS, such as Cloud Foundry or Openshift, target developers and support application patterns. However, some applications require more flexible architectures or design. Roboconf addresses such cases.

With Roboconf, there is no constraint about the programming language, the kind of application or the operating system. You define what you put in your platform, you specify all the interactions, administration procedures and so on.

Roboconf handles application life cycle: hot reconfiguration (e.g. for elasticity issues) and consistency 
(e.g. maintaining a consistent state when a component starts or stops, even accidentally). This relies on a messaging queue 
(currently [Rabbit MQ](https://www.rabbitmq.com)). Application parts know what they expose to and what they depend on from other parts.
The global idea is to apply to applications the concepts used in component technologies like OSGi. Roboconf achieves this in a non-intrusive
way, so that it can work with legacy Software.

<img src="http://roboconf.net/resources/img/roboconf-workflow.png" alt="Roboconf's workflow" style="max-width: 400px;" />

Application parts use the message queue to communicate and take the appropriate actions depending on what is deployed or started.
These *appropriate* actions are executed by plug-ins (such as bash or [Puppet](http://puppetlabs.com)). 

<img src="http://roboconf.net/resources/img/roboconf-architecture-example.jpg" alt="Roboconf's architecture" style="max-width: 400px;" />

Roboconf is distributed technology, based on AMQP 
and REST / JSon. It is IaaS-agnostic, and supports many well-known IaaS (including OpenStack, Amazon Web Services, Microsoft Azure, VMWare, 
as well as a "local" deployment plug-in for on-premise hosts).

More information and tutorials are available on the [web site](http://roboconf.net).  
Build instructions can be found [here](http://roboconf.net/en/sources.html).  
Roboconf is licensed under the terms of the **Apache License v2**.
