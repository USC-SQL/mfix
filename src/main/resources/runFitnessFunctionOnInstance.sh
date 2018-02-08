#!/bin/bash
# *************************** RUN FITNESS FUNCTION ON INSTANCE ***************************
# $1 -> key pair path
# $2 -> instance dns
# $3 -> chromosome json path
# $4 -> local/host directory path to store output files

# transfer chromosome json file to instance
chromosomefilename=$(basename "$3")
chromosomeJsonPath="~/put/$chromosomefilename"
filename="${chromosomefilename%.*}"
rsync -avz --progress $3 -e "ssh -o StrictHostKeyChecking=no -i $1" ubuntu@$2:~/put/

# run jar
ssh -i $1 ubuntu@$2 "java -jar mfix-0.0.1-SNAPSHOT-jar-with-dependencies.jar $chromosomeJsonPath $2 > ~/put/$filename-log.txt 2>&1" -o StrictHostKeyChecking=no

# transfer updated chromosome json file from instance
rsync -avz --progress -e "ssh -o StrictHostKeyChecking=no -i $1" ubuntu@$2:"~/put/$filename*" $4