#!/usr/bin/env bash

set -exuo pipefail

# start openrc
openrc
touch /run/openrc/softlevel

# enable forwarding and restore existing iptables if exist
touch /etc/iptables/rules.v4
echo '
# /etc/conf.d/iptables
IPTABLES_SAVE="/etc/iptables/rules-save"
SAVE_RESTORE_OPTIONS="-c"
SAVE_ON_STOP="yes"
IPFORWARD="yes"
' > /etc/conf.d/iptables
/etc/init.d/iptables save
iptables-restore < /etc/iptables/rules.v4

# enable ip forward in sysctl
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
sysctl -w net.ipv4.ip_forward=1

# init db
sqlite3 yaws.db ".read init.sql"

./gradlew build --no-daemon
