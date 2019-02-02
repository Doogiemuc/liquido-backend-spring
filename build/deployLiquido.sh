#!/bin/sh
#
# Deployment Script for LIQUIDO
#

SSH_KEY=/d/Coding/doogies_credentials/liquido-aws-SSH.pem

BACKEND_DIR=/d/Coding/liquido/liquido-backend-spring
JAR=`ls -t $BACKEND_DIR/target/*.jar | head -1`
BACKEND_USER=ec2-user
BACKEND_HOST=ec2-52-213-79-237.eu-west-1.compute.amazonaws.com
BACKEND_DIR=/home/ec2-user/liquido/liquido-prod
BACKEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:${BACKEND_DIR}
CLIENT_SOURCE=/d/Coding/liquido/liquido-vue-frontend/dist/*
CLIENT_DEST=ec2-user@ec2-52-213-79-237.eu-west-1.compute.amazonaws.com:/var/www/html
CURRENT_DIR=$PWD
MAVEN=$BACKEND_DIR/mvnw

# ===== sanity checks
if [ ! -f $JAR ]; then
	echo "JAR $JAR does not exist!"
	exit 1
fi
if [ ! -d $BACKEND_DIR ]; then
	echo "Cannot find dir $BACKEND_DIR"
	exit 1
fi
if [ ! -d $CLIENT_SOURCE ]; then
	echo "Cannot find dir $CLIENT_SOURCE"
	exit 1
fi
if ! ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} "ls"; then
	echo "Cannot connect to $BACKEND_HOST"
	exit 1
fi


# ===== Checkout master
#cd $TEMP_DIR
#git checkout $REMOTE_REPO

# ===== Build Backend from scratch
echo "===== Rebuild Backend ====="
echo "in $BACKEND_DIR"
read -p "Rebuild Backend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
	cd $BACKEND_DIR
  $MAVEN clean install package
  [ $? -ne 0 ] && exit 1
else
	echo "Backend will NOT be rebuilt."
fi

# ===== Copy latest JAR file to AWS EC2 =====
echo
echo "===== Deploy Backend ====="
echo "from: $JAR"
echo "to:   $BACKEND_DEST"
read -p "Deploy Backend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
	echo "scp -i $SSH_KEY $JAR $BACKEND_DEST"
  scp -i $SSH_KEY $JAR $BACKEND_DEST
  [ $? -ne 0 ] && exit 1
else
	echo "Backend will NOT be deployed."
fi


# =====  Restart liquido-backend-spring  on EC2 instance =====
echo
echo "===== Restart Backend ====="
echo "on $BACKEND_HOST"
read -p "[yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "Stop running backend"
  ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} "pkill -f java.+liquido-backend-spring"
  echo "Start new instance"
  echo "ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} ${BACKEND_DIR}/runLiquido.sh"
  ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} "cd ${BACKEND_DIR};${BACKEND_DIR}/runLiquido.sh"
  [ $? -ne 0 ] && exit 1
  echo "$JAR is booting up on $BACKEND_HOST"
else
 	echo "Backend will not be restarted."
fi



# ===== VueJS Client App =====
echo
echo "===== Deploy WebApp ====="
echo "from: $CLIENT_SOURCE"
echo "to:   $CLIENT_DEST"
read -p "Deploy WebApp? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "scp -i $SSH_KEY -r $CLIENT_SOURCE $CLIENT_DEST"
  scp -i $SSH_KEY -r $CLIENT_SOURCE $CLIENT_DEST
  [ $? -ne 0 ] && exit 1
else
 	echo "WebApp will NOT be deployed."
fi

# ===== Run E2E tests =====





cd $CURRENT_DIR
echo
echo "Finished successfully"