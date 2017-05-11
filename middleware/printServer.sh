#! /bin/bash

printerConfDir="./3Dserver"

echo "printServer.sh: Starting ippserver with configuration $printerConfDir" >> log.txt

./ippserver -C $printerConfDir
