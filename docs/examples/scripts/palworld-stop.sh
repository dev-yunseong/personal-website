#!/bin/sh
set -eu

# Replace this placeholder before installation. `stop` preserves volumes and configuration.
DEPLOY_DIRECTORY="/home/USER/deploy/palworld"
exec docker compose --project-directory "$DEPLOY_DIRECTORY" stop palworld
