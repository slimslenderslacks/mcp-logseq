#!/bin/bash
# Multi-platform Docker build script for mcp-logseq
# Builds for both amd64 and arm64 architectures

set -e

# Configuration
IMAGE_NAME="${IMAGE_NAME:-slimslenderslacks/mcp-logseq}"
TAG="${TAG:-latest}"
PLATFORMS="linux/amd64,linux/arm64"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building multi-platform Docker image${NC}"
echo "Image: ${IMAGE_NAME}:${TAG}"
echo "Platforms: ${PLATFORMS}"
echo ""

# Check if buildx is available
if ! docker buildx version &> /dev/null; then
    echo -e "${RED}Error: docker buildx is not available${NC}"
    echo "Please install Docker Buildx: https://docs.docker.com/buildx/working-with-buildx/"
    exit 1
fi

# Create/use a buildx builder instance
BUILDER_NAME="mcp-logseq-builder"
if ! docker buildx inspect ${BUILDER_NAME} &> /dev/null; then
    echo -e "${YELLOW}Creating new buildx builder: ${BUILDER_NAME}${NC}"
    docker buildx create --name ${BUILDER_NAME} --use
else
    echo -e "${YELLOW}Using existing buildx builder: ${BUILDER_NAME}${NC}"
    docker buildx use ${BUILDER_NAME}
fi

# Bootstrap the builder (download QEMU, etc.)
echo -e "${YELLOW}Bootstrapping builder...${NC}"
docker buildx inspect --bootstrap

# Build arguments
BUILD_ARGS=""
if [ ! -z "$PUSH" ] && [ "$PUSH" = "true" ]; then
    BUILD_ARGS="--push"
    echo -e "${GREEN}Will push to registry after build${NC}"
else
    BUILD_ARGS="--load"
    echo -e "${YELLOW}Will load image locally (single platform only)${NC}"
    echo -e "${YELLOW}To push to registry, run: PUSH=true ./build-multiplatform.sh${NC}"
    # When loading, we can only build for one platform
    PLATFORMS="linux/$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/')"
    echo -e "${YELLOW}Building for current platform only: ${PLATFORMS}${NC}"
fi

# Build the image
echo ""
echo -e "${GREEN}Starting build...${NC}"
docker buildx build \
    --platform ${PLATFORMS} \
    -f Dockerfile.mcp \
    -t ${IMAGE_NAME}:${TAG} \
    ${BUILD_ARGS} \
    .

echo ""
if [ "$PUSH" = "true" ]; then
    echo -e "${GREEN}✓ Multi-platform build complete and pushed!${NC}"
    echo "Image: ${IMAGE_NAME}:${TAG}"
    echo "Platforms: ${PLATFORMS}"
else
    echo -e "${GREEN}✓ Build complete and loaded locally!${NC}"
    echo "Image: ${IMAGE_NAME}:${TAG}"
    echo "Platform: ${PLATFORMS}"
    echo ""
    echo "To build and push for all platforms, run:"
    echo -e "${YELLOW}PUSH=true ./build-multiplatform.sh${NC}"
fi

echo ""
echo "To inspect the image:"
echo "  docker buildx imagetools inspect ${IMAGE_NAME}:${TAG}"
