#!/bin/bash

# It is a loop, but only one directory should be found.
for f in `find /usr/local -maxdepth 1 -type d -name "roboconf*-agent*"`; 
do
	mv $f roboconf-agent;
done
