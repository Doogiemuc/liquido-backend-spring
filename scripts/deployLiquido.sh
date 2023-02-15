#!/bin/bash
#
# Linux Shell - Deployment Script for LIQUIDO
#
# Usage: ./deployLiquido.sh [-j JAVA_HOME] [-s BACKEND_SSH_KEY]

# Load options from configuration file
source deploy-config.sh

# Load command line options
while getopts j:s:p:h? flag
do
    case "${flag}" in
        j) JAVA_HOME=${OPTARG};;
        s) BACKEND_SSH_KEY=${OPTARG};;   # SSH key for backend host
        p) PWA_SSH_KEY=${OPTARG};;       # SSH key for mobile app SCP
        w) WEB_SSH_KEY=${OPTARG};;       # SSH key for mobile app SCP
        h) echo "LIQUIDO CI/CD deployment script"
           echo "Usage: $0 [-j JAVA_HOME] [-s BACKEND_SSH_KEY] [-p PWA_SSH_KEY]"
           exit 1;;
    esac
done

[ -z "$JAVA_HOME" ] && echo "Need -j JAVA_HOME !" && exit 1
[ -z "$BACKEND_SSH_KEY" ] && echo "Need -s BACKEND_SSH_KEY for backend host!" && exit 1
[ -z "$PWA_SSH_KEY" ] && echo "Need -p PWA_BACKEND_SSH_KEY to deploy mobile app!" && exit 1
[ -z "$WEB_SSH_KEY" ] && echo "Need -w WEB_BACKEND_SSH_KEY to update website!" && exit 1


# check that file exists and EXIT with error if not.
# param1 path to file
# param2 description of file for error message
function checkFileExists() {
  if [ ! -f $1 ]; then
      echo "ERROR: $2 $1"
      exit 1
  fi
}

function checkDirExists() {
  if [ ! -d $1 ]; then
      echo "ERROR: $2 $1"
      exit 1
  fi
}


# Backup current directory
CURRENT_DIR=`pwd`

# Source code dir
CODE_DIR=/Users/doogie/Coding/liquido

# Liquido Java Spring Backend
[ -z "$BACKEND_SOURCE" ] && BACKEND_SOURCE=${CODE_DIR}/liquido-backend-spring
BACKEND_USER=ec2-user
BACKEND_HOST=api.liquido.vote                                        # liquido-prod-lightsail static IP  3.72.227.123
BACKEND_API=https://${BACKEND_HOST}:7180/liquido-api/v3              # HTTPS !!!
BACKEND_DEST_DIR=/home/ec2-user/liquido-prod
BACKEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:${BACKEND_DEST_DIR}
checkDirExists $BACKEND_SOURCE "Backend Source"

# Liquido Progressive Web App (PWA)
[ -z "$PWA_SOURCE" ] && PWA_SOURCE=${CODE_DIR}/liquido-mobile-pwa-vue3
#PWA_DEST=${BACKEND_USER}@${BACKEND_HOST}:/var/www/html/liquido-mobile    # liquido INT env on AWS lightsail
#PWA_URL=http://$BACKEND_HOST/liquido-mobile

PWA_HOST=access799372408.webspace-data.io   # ionos webspace
PWA_USER=u98668608
PWA_DEST=${PWA_USER}@${PWA_HOST}:./liquido-mobile-pwa
PWA_URL=https://app.liquido.vote    				# for cypress test

# Cypress configuration for environment
#CYPRESS_CONFIG_FILE=./test/e2e/cypress.prod.json

# Liquido Vue Web frontend
[ -z "$FRONTEND_SOURCE" ] && FRONTEND_SOURCE=${CODE_DIR}/liquido-vue-frontend
FRONTEND_DEST=${BACKEND_USER}@${BACKEND_HOST}:/var/www/html/liquido-web
FRONTEND_URL=http://$BACKEND_HOST
checkDirExists $FRONTEND_SOURCE "Frontend Source"

# Liquido.vote Website
WEB_SOURCE=${CODE_DIR}/FlexStart-landing-page/_site/         # Trailing slash in source is important for rsync command!
WEB_DEST=${PWA_USER}@${PWA_HOST}:./liquido-vote-website
checkDirExists $WEB_SOURCE "Website Source"

# Cypress End-2-End tests against mobile PWA
CYPRESS_CONFIG_FILE=$PWA_SOURCE/cypress.config.PROD.js
checkFileExists $CYPRESS_CONFIG_FILE "Cannot find Cypress Config file"


# Tools
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
if ! ssh -i $BACKEND_SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} "ls" > /dev/null; then
	echo -e "$RED_FAIL"
	exit 1
fi
echo -e "$GREEN_OK"

echo
echo "===== Build Backend ====="
echo
echo "in $BACKEND_SOURCE"
read -p "Build backend? [YES|skipTests|no] " yn
if [[ $yn =~ ^[Ss](kip)?$ ]] ; then
  echo "  Skipping tests."
  cd $BACKEND_SOURCE
  $MAVEN clean install package -DskipTests
  [ $? -ne 0 ] && exit 1
  echo
  echo -e "Backend built successfully. ${GREEN_OK}"
  echo
elif [[ $yn =~ ^[Nn] ]] ; then
  echo "Backend will NOT be built."
else
  cd $BACKEND_SOURCE
  $MAVEN clean install package
  [ $? -ne 0 ] && exit 1
  echo
  echo -e "Backend built successfully. ${GREEN_OK}"
  echo
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
echo "===== Deploy LIQUIDO backend (JAR file) ====="
echo
echo "from: $JAR"
echo "to:   $BACKEND_DEST"
read -p "Upload backend JAR file? [YES|no] " yn
if [[ $yn =~ ^[Nn] ]] ; then
  echo "Backend will NOT be deployed."
else
  echo "scp -i $BACKEND_SSH_KEY $JAR $BACKEND_DEST"
  scp -i $BACKEND_SSH_KEY $JAR $BACKEND_DEST
  [ $? -ne 0 ] && exit 1
  echo -e "Backend deployed successfully. ${GREEN_OK}"
fi

echo
echo "===== Restart Backend ====="
echo
echo "on $BACKEND_DEST"
echo

RESTART_CMD="(cd ${BACKEND_DEST_DIR};./restartLiquido.sh ${JAR_NAME})"

read -p "Restart remote backend? [YES|no] " yn
if [[ $yn =~ ^[Nn] ]] ; then
  echo "Backend will NOT be restarted."
else
  echo "Restarting liquido backend:"
  echo "${RESTART_CMD}"
  echo "--------------------- output of remote command --------------"
  ssh -i $BACKEND_SSH_KEY ${BACKEND_USER}@${BACKEND_HOST} ${RESTART_CMD}
  [ $? -ne 0 ] && exit 1
  echo "---------------------- end of remote command ----------------"
  echo -e "Ok, ${JAR_NAME} is booting up on $BACKEND_HOST ... $GREEN_OK"
fi


if false; then


echo
echo "===== Build Web Frontend ====="
echo
echo "in $FRONTEND_SOURCE"
read -p "Build Web Frontend? [YES|no] " yn
FRONTEND_BUILT_SUCCESSFULLY=false
if [[ $yn =~ ^[Nn] ]] ; then
  echo "Web Frontend will NOT be built."
else
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
read -p "Deploy Web Frontend? [YES|no] " yn
if [[ $yn =~ ^[Nn]$ ]] ; then
  echo "Web Frontend will NOT be deployed."
else
  echo "scp -i $BACKEND_SSH_KEY -r $FRONTEND_SOURCE/dist/* $FRONTEND_DEST"
  scp -i $BACKEND_SSH_KEY -r $FRONTEND_SOURCE/dist/* $FRONTEND_DEST
  [ $? -ne 0 ] && exit 1
  echo -e "Web Frontend deployed to $FRONTEND_DEST ${GREEN_OK}"
fi


fi  ## SKIP




echo
echo "===== Build Mobile PWA ====="
echo
echo "in $PWA_SOURCE"
read -p "Build Mobile PWA? [YES|no] " yn
PWA_BUILT_SUCCESSFULLY=false
if [[ $yn =~ ^[Nn]$ ]] ; then
  echo "Mobile PWA will NOT be built."
else
  cd $PWA_SOURCE
  $NPM run build
  [ $? -ne 0 ] && exit 1
  # echo "setting PWA_BUILT_SUCCESSFULLY to true"
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
read -p "Redeploy PWA? [YES|no] " yn
if [[ $yn =~ ^[Nn]$ ]] ; then
  echo "Mobile PWA will NOT be deployed"
else
  echo "Clean $PWA_DEST/*"
  ssh -i $PWA_SSH_KEY ${PWA_USER}@${PWA_HOST} rm -rf $PWA_DEST/*
  echo

  #echo "Upload PWA: scp -i $PWA_SSH_KEY -r $PWA_SOURCE/dist/* $PWA_DEST"
  #scp -i $PWA_SSH_KEY -r $PWA_SOURCE/dist/* $PWA_DEST

  echo "Upload PWA: rsync -avr -e \"ssh -i $PWA_SSH_KEY\" $PWA_SOURCE/dist/ $PWA_DEST"
  rsync -avr -e "ssh -i $PWA_SSH_KEY" $PWA_SOURCE/dist/ $PWA_DEST    # Trailing slash in source is important!
  echo

  [ $? -ne 0 ] && exit 1
  echo -e "Mobile PWA deployed to $PWA_DEST ${GREEN_OK}"
fi

echo
echo "===== Update Website www.liquido.vote ====="
echo
echo "from: $WEB_SOURCE"
echo "to:   $WEB_DEST"
read -p "Update Website www.liquido.vote? [YES|no] " yn
if [[ $yn =~ ^[Nn]$ ]] ; then
  echo "www.liquido.vote will not be updated"
else

  echo "Updating Website: rsync -avr -e \"ssh -i $WEB_SSH_KEY\" $WEB_SOURCE $WEB_DEST"
  rsync -avr -e "ssh -i $WEB_SSH_KEY" $WEB_SOURCE $WEB_DEST
  echo

  [ $? -ne 0 ] && exit 1
  echo -e "Website updated to $WEB_DEST ${GREEN_OK}"
fi





echo
echo "===== Sanity checks ====="
echo
echo -n "Querying backend to be alive at ${BACKEND_API}/_ping (for max 20 seconds) ..."

PING_SUCCESS=0
for i in {1..20}; do
  # graphQL ping would be: "curl -X POST -d '{"query": "query {ping}"}' -H "Content-Type: application/json" ${BACKEND_API}/graphql
	HELLO_WORD=`curl ${BACKEND_API}/_ping`
	echo "received $HELLO_WORD"
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



echo
echo "===== Mobile PWA: End-2-End Tests ====="
echo
echo "PWA_SOURCE:  $PWA_SOURCE"
echo "PWA_URL:     $PWA_URL"
echo "Backend API: $BACKEND_API"
echo

# Cypress command line --config foo=bar and --env envFoo=envVal  could overwrite values from --config-file
CYPRESS_CMD="$PWA_SOURCE/$CYPRESS run --config-file=$CYPRESS_CONFIG_FILE --config baseUrl=$PWA_URL --env backendBaseURL=$BACKEND_API"

read -p "Run Cypress tests against PWA? [YES|no] " yn
if [[ $yn =~ ^[Nn]$ ]] ; then
  echo "Cypress test will NOT be run."
else
	cd $PWA_SOURCE
	echo $CYPRESS_CMD
	eval $CYPRESS_CMD
	[ $? -ne 0 ] && exit 1
	echo -e "Cypress tests against PWA were successful ${GREEN_OK}"
fi





if false; then

echo
echo "===== Cypress End-2-End Tests against web frontend ====="
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

fi # skip testing old web frontend





cd $CURRENT_DIR
echo
echo -e "${GREEN}Finished successfully${DEFAULT}"
