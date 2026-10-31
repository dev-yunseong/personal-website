#!/bin/sh
set -eu

# Replace this placeholder before installation. Do not put credentials in this script.
DEPLOY_DIRECTORY="/home/USER/deploy/palworld"

if docker compose --project-directory "$DEPLOY_DIRECTORY" ps --status running --services | grep -qx 'palworld'; then
    printf 'Running\n'
else
    printf 'Stopped\n'
fi
