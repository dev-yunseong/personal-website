#!/bin/bash
set -e

DEPLOY_PATH=$HOME/deploy/website

# Deploy on server via SSH
ssh $DEPLOY_USER@$DEPLOY_SERVER << EOF
set -e

# Validate that .env file exists on the server
if [ ! -f "$DEPLOY_PATH/.env" ]; then
  echo "Error: .env file not found at $DEPLOY_PATH/.env" >&2
  exit 1
fi

# Login to GHCR
echo "$GITHUB_TOKEN" | docker login ghcr.io -u "$GITHUB_USERNAME" --password-stdin

# Pull the latest image
docker pull $IMAGE

# Stop and remove existing container
docker stop website 2>/dev/null || true
docker rm website 2>/dev/null || true

# Run new container with env file
docker run -d \
  --name website \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file $DEPLOY_PATH/.env \
  $IMAGE

echo "Deployment complete!"
EOF
