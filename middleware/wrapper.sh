#! /bin/bash

# This is a wrapper getting one argument to start lpr sender with no output

ipAddress="10.0.5.115"
username="admin"
queueName="YSoft.be3D"
fileName=$2
jobData=$1

echo "wrapper.sh: Starging YSoft LprJobSender with arguments: " >> log.txt
echo "wrapper.sh: IP address: $ipAddress" >> log.txt
echo "wrapper.sh: Username: $username" >> log.txt
echo "wrapper.sh: Queue:  $queueName" >> log.txt
echo "wrapper.sh: File Name: $fileName" >> log.txt
echo "wrapper.sh: Job Data: $jobData" >> log.txt

mono ./LprJobSender.exe \
-IPAddress $ipAddress -Username $username -Queue $queueName -Filename $fileName -JobData $jobData
