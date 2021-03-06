#! /bin/bash

dataDirectory="./3dserver_debug/eDee"

echo "watcher.sh: Starting the watch on directory $dataDirectory" >> log.txt

fswatch -e ".*" -i "\\.prn$" $dataDirectory | \
 xargs -n1 ./fileRenamer.sh
