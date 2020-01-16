#!/bin/bash
#
# Linux Shell - Deployment Script for LIQUIDO
#

# export JAVA_HOME=/d/Coding/DevApps/jdk-8.211

SSH_KEY=/d/Coding/doogies_credentials/liquido-aws-SSH.pem

BACKEND_SOURCE=/d/Coding/liquido/liquido-backend-spring
BACKEND_USER=ec2-user
BACKEND_HOST=ec2-52-208-204-181.eu-west-1.compute.amazonaws.com
BACKEND_API=http://${BACKEND_HOST}:80/liquido/v2
BACKEND_DEST_DIR=/home/ec2-user/liquido/liquido-int
BACKEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:${BACKEND_DEST_DIR}

FRONTEND_BASE=/d/Coding/liquido/liquido-vue-frontend
FRONTEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:/var/www/html
FRONTEND_URL=http://$BACKEND_HOST

DOC_SOURCE=/d/Coding/liquido/liquido-doc-gulp-pug/_site/
DOC_DEST=${BACKEND_USER}@${BACKEND_HOST}:/home/ec2-user/liquido/liquido-doc

CURRENT_DIR=$PWD
NPM=npm
MAVEN=./mvnw
CYPRESS=/d/Coding/liquido/liquido-vue-frontend/node_modules/.bin/cypress

# ===== BASH colors =====
DEFAULT="\e[39m"
GREEN="\e[32m"
YELLOW="\e[33m"
RED="\e[31m"
GREEN_OK="${GREEN}OK${DEFAULT}"
RED_FAIL="${RED}FAIL${DEFAULT}"

#
# ===== Deployment Tasks =====
#

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
if [[ $yn =~ ^[Ss](kip)?$ ]] ; then
  echo "  Skipping tests."
  cd $BACKEND_SOURCE
  $MAVEN clean install package -DskipTests
  [ $? -ne 0 ] && exit 1
  echo
  echo -e "Backend built successfully. ${GREEN_OK}"
  echo
elif [[ $yn =~ ^[Yy](es)?$ ]] ; then
  cd $BACKEND_SOURCE
  $MAVEN clean install package
  [ $? -ne 0 ] && exit 1
  echo
  echo -e "Backend built successfully. ${GREEN_OK}"
  echo
else
	echo "Backend will NOT be built."
fi

JAR_NAME=`(cd ${BACKEND_SOURCE}/target; ls -1 -t *.jar | head -1)`
JAR=${BACKEND_SOURCE}/target/${JAR_NAME}

echo
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
  echo "scp -i $SSH_KEY -r $FRONTEND_BASE/dist/* $FRONTEND_DEST"
  scp -i $SSH_KEY -r $FRONTEND_BASE/dist/* $FRONTEND_DEST
  [ $? -ne 0 ] && exit 1
  echo -e "Webapp deployed to $FRONTEND_DEST ${GREEN_OK}"
else
  echo "WebApp will NOT be deployed."
fi

echo
echo "===== Update LIQUIDO Documentation on EC2 ====="
echo
read -p "Update LIQUIDO documentation? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "rsync -avz -e ssh -i $SSH_KEY $DOC_SOURCE $DOC_DEST"
  BACKUP_CURRENT_DIR=$PWD
  cd $DOC_SOURCE
  rsync -avz -e "ssh -i $SSH_KEY" . $DOC_DEST
  [ $? -ne 0 ] && exit 1
  cd $BACKUP_CURRENT_DIR
  echo -e "Documentation updated in $DOC_DEST ${GREEN_OK}"
else
  echo "Documentation will NOT be updated."
fi


echo
echo "===== Sanity checks ====="
echo
echo -n "Waiting (max 20 secs) for backend to be alive ..."

PING_SUCCESS=0
for i in {1..20}; do
	HELLO_WORD=`curl -s -X GET ${BACKEND_API}/_ping`
	if [[ $HELLO_WORD = '{"Hello":"World"}' ]] ; then
	    echo -e " $GREEN_OK"
		PING_SUCCESS=1
		break;
	fi
	echo -n "."
	sleep 1
done
if [ $PING_SUCCESS == 0 ]; then
	echo -e "$RED_FAIL"
	exit 1
fi


#echo -n "Login with dummy SMS token should NOT be possible in PROD ... "
#
#DUMMY_LOGIN=`curl -s -X GET "${BACKEND_API}/auth/loginWithSmsToken?mobile=%2B491234567890&token=998877"`
#if [[ $DUMMY_LOGIN != *'"httpStatus":401'* ]] ; then
#	echo -e "$RED_FAIL"
#	exit 1
#fi
#echo -e "$GREEN_OK"

#
# ===== Upsert School Test Data =====
#


echo
echo "===== End-2-End Tests ====="
echo
echo Frontend: $FRONTEND_URL
echo Backend:  $BACKEND_API
echo
read -p "Run Cypress Happy Case test? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
	cd $FRONTEND_BASE
	echo "$CYPRESS run --config baseUrl=$FRONTEND_URL --env backendBaseURL=$BACKEND_API --spec ./cypress/integration/liquidoTests/liquidoHappyCase.js"
	$CYPRESS run --config baseUrl=$FRONTEND_URL --env backendBaseURL=$BACKEND_API --spec ./cypress/integration/liquidoTests/liquidoHappyCase.js
	[ $? -ne 0 ] && exit 1
	fecho -e "Tests successfull ${GREEN_OK}"
fi

cd $CURRENT_DIR
echo
echo -e "${GREEN}Finished successfully${DEFAULT}"
