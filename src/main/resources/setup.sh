#!/bin/bash

# $1 -> key-pair path
# $2 -> instance dns
# $3 -> subject path
# $4 -> mfix object json path
# #5 -> mfix jar path

#kill all chrome and chromedriver processes
#killall -9 chrome
#killall -9 chromedriver

# copy subject contents to the put folder on the instance
ssh -o StrictHostKeyChecking=no -i "$1" ubuntu@$2 "sudo /opt/tomcat/bin/startup.sh; rm -rf ~/*; mkdir ~/put;"
rsync -avz --exclude "output*" --progress $3 -e "ssh -o StrictHostKeyChecking=no -i $1" ubuntu@$2:~/put/

# copy mfix jar
jarname=$(basename "$5")
rsync -avz --progress $5 -e "ssh -o StrictHostKeyChecking=no -i $1" ubuntu@$2:~/
ssh -o StrictHostKeyChecking=no -i "$1" ubuntu@$2 "chmod +x ~/$jarname"

# copy mfix json
rsync -avz --progress $4 -e "ssh -o StrictHostKeyChecking=no -i $1" ubuntu@$2:~/put/