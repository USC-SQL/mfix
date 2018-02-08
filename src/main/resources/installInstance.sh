#!/bin/bash

#install java
sudo apt-add-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
export JAVA_HOME=/usr/lib/jvm/java-8-oracle
export PATH=${PATH}:${JAVA_HOME}/bin

#install chrome
wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
sudo sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list'
sudo apt-get update 
sudo apt-get install google-chrome-stable

#install tomcat
cd /tmp
curl -O http://apache.ip-guide.com/tomcat/tomcat-9/v9.0.0.M22/bin/apache-tomcat-9.0.0.M22.tar.gz
sudo mkdir /opt/tomcat
sudo tar xzvf apache-tomcat-9.0.0.M22.tar.gz -C /opt/tomcat --strip-components=1
cd /opt/tomcat

# copy mfix jar to the instance

#sudo vi /opt/tomcat/conf/Catalina/localhost/put.xml
#<Context displayName="put" 
#     docBase="/home/ubuntu/put"
#     path="/put"
#     reloadable="true" />

#sudo /opt/tomcat/bin/startup.sh
#sudo /opt/tomcat/bin/shutdown.sh