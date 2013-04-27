#!/usr/bin/env bash
JAVA_HOME=/home/exampleuser/lib/java/jdk1.6.0_30
export JAVA_HOME
PATH=$PATH:$JAVA_HOME/bin

#execute buildr command from trieservice folder
pushd TrieService
sudo rm -f trieservice.jar
sudo env JAVA_HOME=$JAVA_HOME buildr clean package
popd

#copy upstart to /etc/init
sudo cp -fv trieservice.conf /etc/init/trieservice.conf

#start service
sudo service trieservice start
