# Building mcp-logseq Docker Images

Quick reference for building Docker images.

## Single Platform (Fast)

Build for your current architecture only:

```bash
docker build -f Dockerfile.mcp -t slimslenderslacks/mcp-logseq:latest .
```

## Multi-Platform (Both amd64 and arm64)

### Local Testing

Build for current platform and load locally:
```bash
./build-multiplatform.sh
```

### Push to Registry

Build for all platforms and push to Docker Hub:
```bash
PUSH=true ./build-multiplatform.sh
```

### Custom Tag

```bash
TAG=v1.0.0 PUSH=true ./build-multiplatform.sh
```

## Verify Multi-Platform Image

After pushing, verify both architectures are available:

```bash
docker buildx imagetools inspect slimslenderslacks/mcp-logseq:latest
```

## Supported Platforms

- **linux/amd64** - Intel/AMD 64-bit
- **linux/arm64** - ARM 64-bit (Apple Silicon, AWS Graviton)

## See Also

- [Multi-Platform Build Guide](docs/MULTIPLATFORM_BUILD.md) - Comprehensive documentation
- [GitHub Actions](.github/workflows/docker-build.yml) - Automated CI/CD builds
