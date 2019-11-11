#!/bin/sh
#
# Deployment Script for LIQUIDO
#

SSH_KEY=/d/Coding/doogies_credentials/liquido-aws-SSH.pem

BACKEND_SOURCE=/d/Coding/liquido/liquido-backend-spring
BACKEND_USER=ec2-user
BACKEND_HOST=ec2-34-255-196-76.eu-west-1.compute.amazonaws.com
BACKEND_DEST_DIR=/home/ec2-user/liquido/liquido-int
BACKEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:${BACKEND_DEST_DIR}

FRONTEND_BASE=/d/Coding/liquido/liquido-vue-frontend
FRONTEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:/var/www/html

CURRENT_DIR=$PWD
NPM=npm
MAVEN=./mvnw

# ===== BASH colors
DEFAULT="\e[39m"
GREEN="\e[32m"
YELLOW="\e[33m"
RED="\e[31m"
GREEN_OK="${GREEN}OK${DEFAULT}"
RED_FAIL="${RED}FAIL${DEFAULT}"

echo
echo " ===== Preparing to Deploying LIQUIDO ====="
echo

echo -n "Checking for frontend in $FRONTEND_BASE ..."
if [ ! -d $FRONTEND_BASE ]; then
	echo -e "$RED_FAIL"
	exit 1
fi
echo -e "$GREEN_OK"

echo -n "Checking for backend in $BACKEND_SOURCE ..."
if [ ! -d $BACKEND_SOURCE ]; then
	echo -e "$RED_FAIL"
	exit 1
fi
echo -e "$GREEN_OK"


echo -n "Checking connection to target host $BACKEND_HOST ..."
if ! ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} "ls" > /dev/null; then
	echo -e "$RED_FAIL"
	exit 1
fi
echo -e "$GREEN_OK"

echo
echo "===== Build Backend ====="
echo
echo "in $BACKEND_SOURCE"
read -p "Build backend? [yes|skipTests|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  cd $BACKEND_SOURCE
  $MAVEN clean install package
  [ $? -ne 0 ] && exit 1
  echo -e "Backend built successfully. ${GREEN_OK}"
else
	echo "Backend will NOT be built."
fi

JAR_NAME=`(cd ${BACKEND_SOURCE}/target; ls -1 -t *.jar | head -1)`
JAR=${BACKEND_SOURCE}/target/${JAR_NAME}

echo -n "Checking for backend JAR in $JAR ..."
if [ ! -f $JAR ]; then
  echo -e "$RED_FAIL"
  exit 1
fi
echo -e "$GREEN_OK"

echo
echo "===== Build Frontend ====="
echo
echo "in $FRONTEND_BASE"
read -p "Build Frontend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  cd $FRONTEND_BASE
  $NPM run build
  [ $? -ne 0 ] && exit 1
  echo -e "Frontend built successfully. ${GREEN_OK}"
fi

echo
echo "===== Copy backend JAR file to AWS EC2 ====="
echo
echo "from: $JAR"
echo "to:   $BACKEND_DEST"
read -p "Deploy Backend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "scp -i $SSH_KEY $JAR $BACKEND_DEST"
  scp -i $SSH_KEY $JAR $BACKEND_DEST
  [ $? -ne 0 ] && exit 1
  echo -e "Backend deployed successfully. ${GREEN_OK}"
else
  echo "Backend will NOT be deployed."
fi

echo
echo "===== Restart Backend ====="
echo
BACKEND_START_CMD="${BACKEND_DEST_DIR}/runLiquido.sh ${BACKEND_DEST_DIR}/${JAR_NAME}"
echo "with script: ${BACKEND_USER}@${BACKEND_HOST} ${BACKEND_START_CMD}"
read -p "Restart backend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "Stop running backend"
  ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} "pkill -f java.+liquido-backend-spring"
  echo "Start new instance"
  echo "ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} ${BACKEND_START_CMD}"
  echo "---------------------"
  ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} ${BACKEND_START_CMD}
  [ $? -ne 0 ] && exit 1
  echo "---------------------"
  echo -e "${JAR_NAME} is booting up on $BACKEND_HOST ... ${GREEN_OK}"
else
  echo "Backend will not be restarted."
fi

echo
echo "===== Deploy VueJS Frontend WebApp ====="
echo
echo "from: $FRONTEND_BASE/dist"
echo "to:   $FRONTEND_DEST"
read -p "Deploy WebApp? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "scp -i $SSH_KEY -r $FRONTEND_BASE/dist/. $FRONTEND_DEST"
  scp -i $SSH_KEY -r $FRONTEND_BASE/dist/. $FRONTEND_DEST
  [ $? -ne 0 ] && exit 1
  echo "OK, frontend deployed to $FRONTEND_DEST"
else
  echo "WebApp will NOT be deployed."
fi




echo
echo "===== Sanity checks ====="
echo
echo -n "Ping backend ... "
HELLO_WORD=`curl -s -X GET http://${BACKEND_HOST}:8080/liquido/v2/_ping`
if [[ ! $HELLO_WORD = '{"Hello":"World"}' ]] ; then
	echo -e "$RED_FAIL"
	exit 1
fi
echo -e "$GREEN_OK"

echo -n "Login with dummy SMS token should NOT be possible in PROD ..."

DUMMY_LOGIN=`curl -s -X GET "http://${BACKEND_HOST}:8080/liquido/v2/auth/loginWithSmsToken?mobile=%2B491234567890&code=998877"`
if [[ $DUMMY_LOGIN != *'"httpStatus":401'* ]] ; then
	echo -e "$RED_FAIL"
	exit 1
fi
echo -e "$GREEN_OK"



#TODO: ===== Run Cypress E2E tests against PROD =====



cd $CURRENT_DIR
echo
echo -e "${GREEN}Finished successfully${DEFAULT}"