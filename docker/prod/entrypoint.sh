#!/usr/bin/env bash

set -exuo pipefail

# start openrc
openrc
touch /run/openrc/softlevel

# enable ip forward in sysctl
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
sysctl -w net.ipv4.ip_forward=1

# run spring application
java -jar yaws-0.0.1-SNAPSHOT.jar