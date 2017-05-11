#! /bin/bash

echo "Startup.sh: Startup process initiated" >> log.txt

echo "Startup.sh: Starting watcher on data directory" >> log.txt
./watcher.sh &

echo "Startup.sh: Starting print server" >> log.txt
./printServer.sh
