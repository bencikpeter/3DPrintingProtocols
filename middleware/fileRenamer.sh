#! /bin/bash

echo "fileRenamer.sh: Starting renaming process" >> log.txt

absoluteFilePath=$1

fileName="${absoluteFilePath##*/}" #Absolute path to file

path="${absoluteFilePath%/*}" #Absolute path to directory containing file

newFileName="${fileName#[0-9]-}" #FileName stripped of number prefix: 1-job_zip.prn --> job_zip.prn

newFileNameWithoutExtenton="${newFileName%_*}" #FileName stripped of postfix and extention: job_zip.prn -> job

newFileNameWithExtention="$newFileNameWithoutExtenton.zip" #Extention added: job --> job.zip

newFilePath="$path/$newFileNameWithExtention" #Absolute path to renamed file

if mv "$absoluteFilePath" "$newFilePath" 2> /dev/null; then #example: 1-textjob_zip.prn  --> textjob.zip
    echo "fileRenamer.sh: Rename executed" >> log.txt
    echo "fileRenamer.sh: Original name: $fileName ; New name: $newFileNameWithExtention" >>log.txt
    ./wrapper.sh $newFilePath $newFileNameWithoutExtenton
else
  echo "fileRenamer.sh: Rename not executed: File was already renamed" >> log.txt
fi
