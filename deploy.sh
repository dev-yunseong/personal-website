#!/bin/bash
set -e

# build
./gradlew clean build

DEPLOY_PATH=deploy/website

PROJECT_ROOT=$(pwd)
BUILD_PATH=$(ls $PROJECT_ROOT/build/libs/*.jar)
JAR_NAME=$(basename $BUILD_PATH)

# Copy To Server
scp -P 2222 $BUILD_PATH $DEPLOY_USER@$DEPLOY_SERVER:$DEPLOY_PATH/app.jar

# Run Application
ssh -p 2222 $DEPLOY_USER@$DEPLOY_SERVER << EOF
echo $DEPLOY_PW | sudo -S systemctl set-environment APPLICATION_USER=$APPLICATION_USER APPLICATION_PW=$APPLICATION_PW DATABASE_USER=$DATABASE_USER DATABASE_PW=$DATABASE_PW
echo $DEPLOY_PW | sudo -S systemctl restart website.service
EOF
