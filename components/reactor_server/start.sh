#!/bin/bash

# Start the reactor server

pid=$(ps -ef | grep reactor_server | grep -v grep | awk '{print $2}')
if [ -n "$pid" ]; then
    echo "reactor_server is already running, pid: $pid.kill it"
    kill -9 $pid
fi

nohup ./wd_reactor_server 1504 &