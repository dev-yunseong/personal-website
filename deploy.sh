#!/bin/bash
set -e

# build
./gradlew clean build

DEPLOY_PATH=deploy/website

PROJECT_ROOT=$(pwd)
BUILD_PATH=$(ls $PROJECT_ROOT/build/libs/*.jar)
JAR_NAME=$(basename $BUILD_PATH)

# Copy To Server
ssh $DEPLOY_USER@$DEPLOY_SERVER "mkdir -p $DEPLOY_PATH"
scp $BUILD_PATH $DEPLOY_USER@$DEPLOY_SERVER:$DEPLOY_PATH/app.jar

# Run Application
ssh $DEPLOY_USER@$DEPLOY_SERVER << EOF
export XDG_RUNTIME_DIR=/run/user/\$(id -u)

systemctl --user set-environment APPLICATION_USER=$APPLICATION_USER
systemctl --user set-environment APPLICATION_PW=$APPLICATION_PW
systemctl --user set-environment DATABASE_USER=$DATABASE_USER
systemctl --user set-environment DATABASE_PW=$DATABASE_PW

systemctl --user restart website.service
EOF
