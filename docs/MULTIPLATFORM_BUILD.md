# Multi-Platform Docker Builds

This document explains how to build the mcp-logseq Docker image for multiple platforms (amd64 and arm64).

## Overview

The Docker image is built for two architectures:
- **linux/amd64** - Intel/AMD 64-bit (most cloud servers, Intel Macs)
- **linux/arm64** - ARM 64-bit (Apple Silicon Macs, AWS Graviton, Raspberry Pi 4+)

## Prerequisites

### 1. Install Docker Buildx

Docker Buildx is included in Docker Desktop 19.03+ and Docker Engine 19.03+.

Verify installation:
```bash
docker buildx version
```

If not available, follow the [official installation guide](https://docs.docker.com/buildx/working-with-buildx/).

### 2. Enable QEMU for Cross-Platform Builds

Docker Desktop includes QEMU automatically. For Linux:

```bash
docker run --privileged --rm tonistiigi/binfmt --install all
```

## Building Multi-Platform Images

### Quick Start

**Build for local testing (current platform only):**
```bash
./build-multiplatform.sh
```

**Build and push to Docker Hub (all platforms):**
```bash
PUSH=true ./build-multiplatform.sh
```

### Custom Image Name and Tag

```bash
IMAGE_NAME=myorg/mcp-logseq TAG=v1.0.0 PUSH=true ./build-multiplatform.sh
```

## Build Script Options

The `build-multiplatform.sh` script supports the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `IMAGE_NAME` | Docker image name | `slimslenderslacks/mcp-logseq` |
| `TAG` | Image tag | `latest` |
| `PUSH` | Push to registry after build | `false` |

## How It Works

### Dockerfile Changes

The Dockerfile uses build arguments to support cross-compilation:

```dockerfile
FROM --platform=$BUILDPLATFORM golang:1.23-bookworm AS go-builder

ARG BUILDPLATFORM
ARG TARGETPLATFORM
ARG TARGETOS
ARG TARGETARCH

# Build for target architecture
RUN CGO_ENABLED=0 GOOS=${TARGETOS} GOARCH=${TARGETARCH} go build -o mcp-logseq-server .
```

### Build Process

1. **Builder Creation**: Creates or uses a buildx builder instance
2. **Bootstrap**: Downloads QEMU emulators for cross-platform support
3. **Build**: Compiles Go code for each target architecture
4. **Push/Load**: Either pushes to registry or loads locally

### Platform Detection

When building locally (`PUSH=false`), the script automatically detects your platform:
- macOS on Apple Silicon → `linux/arm64`
- macOS on Intel → `linux/amd64`
- Linux x86_64 → `linux/amd64`
- Linux ARM64 → `linux/arm64`

## Manual Build Commands

If you prefer manual control:

### Local Build (Single Platform)

```bash
# Create builder if needed
docker buildx create --name mcp-builder --use

# Build for current platform
docker buildx build \
  --platform linux/$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/') \
  -f Dockerfile.mcp \
  -t slimslenderslacks/mcp-logseq:latest \
  --load \
  .
```

### Multi-Platform Build and Push

```bash
# Build and push for both platforms
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f Dockerfile.mcp \
  -t slimslenderslacks/mcp-logseq:latest \
  --push \
  .
```

## Inspecting Multi-Platform Images

After pushing a multi-platform image, inspect it:

```bash
docker buildx imagetools inspect slimslenderslacks/mcp-logseq:latest
```

Output example:
```
Name:      slimslenderslacks/mcp-logseq:latest
MediaType: application/vnd.docker.distribution.manifest.list.v2+json
Digest:    sha256:abc123...

Manifests:
  Name:      slimslenderslacks/mcp-logseq:latest@sha256:def456...
  MediaType: application/vnd.docker.distribution.manifest.v2+json
  Platform:  linux/amd64

  Name:      slimslenderslacks/mcp-logseq:latest@sha256:ghi789...
  MediaType: application/vnd.docker.distribution.manifest.v2+json
  Platform:  linux/arm64
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build Multi-Platform Docker Image

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up QEMU
        uses: docker/setup-qemu-action@v2

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: .
          file: ./Dockerfile.mcp
          platforms: linux/amd64,linux/arm64
          push: true
          tags: slimslenderslacks/mcp-logseq:latest
```

## Troubleshooting

### Build is slow

Cross-platform builds use QEMU emulation which is slower than native builds. Expect:
- Native build: 5-10 minutes
- Cross-platform build: 15-30 minutes

### "failed to solve: failed to load cache key"

Clear the build cache:
```bash
docker buildx prune -af
```

### "multiple platforms feature is currently not supported for docker driver"

You need to create a buildx builder:
```bash
docker buildx create --name mcp-builder --use
```

### Cannot load multi-platform image locally

Docker can only load one platform at a time. Either:
1. Build for single platform: `./build-multiplatform.sh` (default)
2. Push to registry: `PUSH=true ./build-multiplatform.sh`

## Best Practices

1. **Test locally first**: Build for your platform before pushing
2. **Tag appropriately**: Use semantic versioning (v1.0.0, v1.0.1, etc.)
3. **Cache layers**: The Dockerfile is optimized to cache NPM dependencies
4. **Monitor build time**: Multi-platform builds take longer

## Performance Notes

| Architecture | Use Case | Build Time |
|-------------|----------|------------|
| linux/amd64 | Cloud servers, Intel Macs | ~10 min |
| linux/arm64 | Apple Silicon, AWS Graviton | ~10 min |
| Both (emulated) | Cross-platform via QEMU | ~25 min |

## Platform-Specific Considerations

### Apple Silicon (M1/M2/M3)

- Native arm64 builds are fast
- amd64 builds use Rosetta 2 (slower but still faster than QEMU)

### Intel/AMD Systems

- Native amd64 builds are fast
- arm64 builds require QEMU emulation (slower)

## See Also

- [Docker Buildx Documentation](https://docs.docker.com/buildx/)
- [Multi-Platform Images](https://docs.docker.com/build/building/multi-platform/)
- [QEMU User Emulation](https://www.qemu.org/docs/master/user/main.html)
