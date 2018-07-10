#!/bin/ksh

print "Stopping MASTER MACHINE"
~/spark-2.3.1-bin-hadoop2.7/sbin/stop-master.sh

print "Stopping SLAVE MACHINES"
~/spark-2.3.1-bin-hadoop2.7/sbin/stop-slaves.sh




