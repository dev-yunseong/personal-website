#!/bin/sh
set -eu

# Replace this placeholder before installation. The Compose file owns environment variables.
DEPLOY_DIRECTORY="/home/USER/deploy/palworld"
exec docker compose --project-directory "$DEPLOY_DIRECTORY" up --detach palworld
