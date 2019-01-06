#!/bin/sh
#
# Deployment Script for LIQUIDO
#

SOURCE_DIR=../target
JAR=`ls -t $SOURCE_DIR/*.jar | head -1`
DEST=ec2-user@ec2-52-213-79-237.eu-west-1.compute.amazonaws.com:/home/ec2-user/liquido/liquido-backend-spring/
SSH_KEY=/d/Coding/doogies_credentials/liquido-aws-SSH.pem

# update to master   (Do a clean checkout from scratch?)
#git pull

# package new JAR file  and run tests
#mvn package

# Copy latest JAR file to AWS EC2
echo "Copying $JAR to $DEST"
scp -i $SSH_KEY $JAR $DEST

# Restart liquido-backend-spring  on EC2 instance
#ssh 'kill ...'
#ssh "java -Dspring.profiles.active=prod -jar $JAR"


#TODO: deploy client