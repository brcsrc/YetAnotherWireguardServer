#!/usr/bin/env bash

set -exuo pipefail

# start openrc
openrc
touch /run/openrc/softlevel

# iptables rules are not persisted or restored here. every rule a network needs is carried in
# that network's wireguard config as PostUp/PostDown hooks, so wg-quick installs them when the
# interface comes up and removes them when it goes down. the config file is the persistence,
# which keeps kernel state from drifting out of sync with it across restarts.

# enable ip forward in sysctl
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
sysctl -w net.ipv4.ip_forward=1

echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") INFO [yaws-tests] test container started and ready for tests"
tail -f /dev/null
