#!/bin/bash
#
# Linux Shell - Deployment Script for LIQUIDO
#
# deployLiquido.sh <path-to-ssh-key.pem>


[ -z "$JAVA_HOME" ] && echo "Need JAVA_HOME !" && exit 1

if [ -n "$1" ] ; then
  SSH_KEY=$1
fi

[ -z "$SSH_KEY" ] && echo "Need SSH_KEY !" && exit 1

CODE_DIR=/Users/doogie/Coding/liquido

# Liquido Java Spring Backend
[ -z "$BACKEND_SOURCE" ] && BACKEND_SOURCE=${CODE_DIR}/liquido-backend-spring
BACKEND_USER=ec2-user
BACKEND_HOST=52.59.209.46        # liquido-prod-lightsail
BACKEND_API=http://${BACKEND_HOST}:80/liquido-api/v3
BACKEND_DEST_DIR=/home/ec2-user/liquido-prod
BACKEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:${BACKEND_DEST_DIR}

# Liquido Vue Web frontend
[ -z "$FRONTEND_SOURCE" ] && FRONTEND_SOURCE=${CODE_DIR}/liquido-vue-frontend
FRONTEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:/var/www/html/liquido-web
FRONTEND_URL=http://$BACKEND_HOST

# Liquido Progressive Web App (PWA)
[ -z "$PWA_SOURCE" ] && PWA_SOURCE=${CODE_DIR}/liquido-mobile-pwa
PWA_DEST=${BACKEND_USER}@${BACKEND_HOST}:/var/www/html/liquido-mobile
PWA_URL=http://$BACKEND_HOST/liquido-mobile

# Cypress configuration for environment
CYPRESS_CONFIG_FILE=./cypress/cypress.int.json

# Liquido Documentation
DOC_SOURCE=${CODE_DIR}/liquido-doc-gulp-pug/_site/
DOC_DEST=${BACKEND_USER}@${BACKEND_HOST}:/home/ec2-user/liquido/liquido-doc

# Tools
CURRENT_DIR=$PWD
NPM=npm
MAVEN=./mvnw
CYPRESS=./node_modules/cypress/bin/cypress

# ===== BASH colors =====
DEFAULT="\e[39m"
GREEN="\e[32m"
YELLOW="\e[33m"
RED="\e[31m"
GREEN_OK="${GREEN}OK${DEFAULT}"
RED_FAIL="${RED}FAIL${DEFAULT}"

echo
echo " =============================== "
echo " ===== LIQUIDO CI pipeline ===== "
echo " =============================== "
echo
echo " BACKEND_SOURCE:  $BACKEND_SOURCE"
echo " BACKEND_DEST:    $BACKEND_DEST"
echo " BACKEND_API:     $BACKEND_API"
echo
echo " FRONTEND_SOURCE: $FRONTEND_SOURCE"
echo " FRONTEND_DEST:   $FRONTEND_DEST"
echo " FRONTEND_URL:    $FRONTEND_URL"
echo

echo
echo "===== Preparing to Deploying LIQUIDO ====="
echo

echo -n "Checking for frontend in $FRONTEND_SOURCE ..."
if [ ! -d $FRONTEND_SOURCE ] ; then
	echo -e "$RED_FAIL"
	exit 1
fi
echo -e "$GREEN_OK"

echo -n "Checking for backend in $BACKEND_SOURCE ... "
if [ ! -d $BACKEND_SOURCE ] ; then
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
if [ ! -f $JAR ] ; then
  echo -e "$RED_FAIL"
  exit 1
fi
echo -e "$GREEN_OK"


echo
echo "===== Upload backend JAR file to AWS EC2 ====="
echo
echo "from: $JAR"
echo "to:   $BACKEND_DEST"
read -p "Upload backend JAR file? [yes|NO] " yn
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

RESTART_CMD="(cd ${BACKEND_DEST_DIR};./restartLiquido.sh ${JAR_NAME})"

read -p "Restart remote backend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "Restarting liquido backend:"
  echo "${RESTART_CMD}"
  echo "--------------------- output of remote command --------------"
  ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} ${RESTART_CMD}
  [ $? -ne 0 ] && exit 1
  echo "---------------------- end of remote command ----------------"
  echo -e "Ok, ${JAR_NAME} is booting up on $BACKEND_HOST ... $GREEN_OK"
else
  echo "Backend will NOT be restarted."
fi



echo
echo "===== Build Web Frontend ====="
echo
echo "in $FRONTEND_SOURCE"
read -p "Build Web Frontend? [yes|NO] " yn
FRONTEND_BUILT_SUCCESSFULLY=false
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  cd $FRONTEND_SOURCE
  $NPM run build
  [ $? -ne 0 ] && exit 1
  FRONTEND_BUILT_SUCCESSFULLY=true
  echo -e "Web Frontend built successfully. ${GREEN_OK}"
fi

echo
echo "===== Deploy Web Frontend ====="
echo
echo "from: $FRONTEND_SOURCE/dist"
echo "to:   $FRONTEND_DEST"
if [ "$FRONTEND_BUILT_SUCCESSFULLY" = false ] ; then
  echo "WARN: You did not build the frontend. This would deploy the last built version!"
fi
read -p "Deploy Web Frontend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "scp -i $SSH_KEY -r $FRONTEND_SOURCE/dist/* $FRONTEND_DEST"
  scp -i $SSH_KEY -r $FRONTEND_SOURCE/dist/* $FRONTEND_DEST
  [ $? -ne 0 ] && exit 1
  echo -e "Web Frontend deployed to $FRONTEND_DEST ${GREEN_OK}"
else
  echo "Web Frontend will NOT be deployed."
fi




echo
echo "===== Build Mobile PWA ====="
echo
echo "in $PWA_SOURCE"
read -p "Build Mobile PWA? [yes|NO] " yn
PWA_BUILT_SUCCESSFULLY=false
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  cd $PWA_SOURCE
  $NPM run build
  [ $? -ne 0 ] && exit 1
  echo "setting PWA_BUILT_SUCCESSFULLY to true"
  PWA_BUILT_SUCCESSFULLY=true
  echo -e "Mobile PWA built successfully. ${GREEN_OK}"
fi


echo
echo "===== Deploy Mobile PWA ====="
echo
echo "from: $PWA_SOURCE/dist"
echo "to:   $PWA_DEST"
if [ "$PWA_BUILT_SUCCESSFULLY" = false ] ; then
  echo "WARN: You did not build the mobile PWA. This would redeploy the last built version!"
fi
read -p "Redeploy PWA? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
  echo "Clean $PWA_DEST/*"
  ssh -i $SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} rm -rf $PWA_DEST/*
  echo "Upload PWA: scp -i $SSH_KEY -r $PWA_SOURCE/dist/* $PWA_DEST"
  scp -i $SSH_KEY -r $PWA_SOURCE/dist/* $PWA_DEST
  [ $? -ne 0 ] && exit 1
  echo -e "Mobile PWA deployed to $PWA_DEST ${GREEN_OK}"
else
  echo "Mobile PWA will NOT be deployed."
fi



echo
echo "===== Sanity checks ====="
echo
echo -n "Querying backend to be alive at $BACKEND_API ..."

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
if [ $PING_SUCCESS == 0 ] ; then
	echo -e "$RED_FAIL"
	exit 1
fi

#
# TODO: make some security checks against PROD
#



echo
echo "===== Mobile PWA: End-2-End Tests ====="
echo
echo "PWA_SOURCE:  $PWA_SOURCE"
echo "PWA_URL:     $PWA_URL"
echo "Backend API: $BACKEND_API"
echo

# Cypress command line --config and --env overwrite --config-file
CYPRESS_CMD="$PWA_SOURCE/$CYPRESS run --config baseUrl=$PWA_URL --env LIQUIDO_API=$BACKEND_API --config-file=$CYPRESS_CONFIG_FILE --spec ./cypress/integration/happy-case.js"

read -p "Run Cypress tests against PWA? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
	cd $PWA_SOURCE
	echo $CYPRESS_CMD
	eval $CYPRESS_CMD
	[ $? -ne 0 ] && exit 1
	echo -e "Cypress tests against PWA were successful ${GREEN_OK}"
fi




echo
echo "===== Web Frontend: End-2-End Tests ====="
echo
echo "Frontend Source: $FRONTEND_SOURCE"
echo "Web frontend:    $FRONTEND_URL"
echo "Backend API:     $BACKEND_API"
echo

CYPRESS_CMD="$FRONTEND_SOURCE/$CYPRESS run --config baseUrl=$FRONTEND_URL --env backendBaseURL=$BACKEND_API --spec ./cypress/integration/liquidoTests/liquidoHappyCase.js"

read -p "Run Cypress tests against web frontend? [yes|NO] " yn
if [[ $yn =~ ^[Yy](es)?$ ]] ; then
	cd $FRONTEND_SOURCE
	echo $CYPRESS_CMD
	eval $CYPRESS_CMD
	[ $? -ne 0 ] && exit 1
	echo -e "Cypress tests against web frontend were successful ${GREEN_OK}"
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



cd $CURRENT_DIR
echo
echo -e "${GREEN}Finished successfully${DEFAULT}"
