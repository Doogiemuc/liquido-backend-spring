#!/bin/sh
#
# Deployment Script for LIQUIDO
#

SSH_KEY=/d/Coding/doogies_credentials/liquido-aws-SSH.pem

BACKEND_DIR=/d/Coding/liquido/liquido-backend-spring
JAR=`ls -t $BACKEND_DIR/target/*.jar | head -1`
BACKEND_DEST=ec2-user@ec2-52-213-79-237.eu-west-1.compute.amazonaws.com:/home/ec2-user/liquido/liquido-backend-spring/

CLIENT_SOURCE=/d/Coding/liquido/liquido-vue-frontend/dist/*
CLIENT_DEST=ec2-user@ec2-52-213-79-237.eu-west-1.compute.amazonaws.com:/var/www/html

# ===== VueJS Client App =====

# update to master   (Do a clean checkout from scratch?)
#cd $BACKEND_DIR
#git checkout $REMOTE_REPO

# package new JAR file AND run tests
#mvn clean install
#mvn package

# Copy latest JAR file to AWS EC2
echo "===== Deploy Backend: $JAR => $BACKEND_DEST"
read -p "Deploy Backend? [yes|NO] " yn
case $yn in
	[Yy]* )
     echo "scp -i $SSH_KEY $JAR $BACKEND_DEST";
     scp -i $SSH_KEY $JAR $BACKEND_DEST;
     break;;
  * ) echo "Backend NOT deployed.";;
esac


# ===== VueJS Client App =====
echo "===== Deploy WebApp: $CLIENT_SOURCE => CLIENT_DEST"
echo "scp -i $SSH_KEY -r $CLIENT_SOURCE $CLIENT_DEST"
# scp -i $SSH_KEY -r $CLIENT_SOURCE $CLIENT_DEST

# =====  Restart liquido-backend-spring  on EC2 instance =====
#ssh 'kill ...'
#ssh "java -Dspring.profiles.active=prod -jar $JAR"
