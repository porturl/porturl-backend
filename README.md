# porturl-backend

[![Build Status](https://github.com/porturl/porturl-backend/actions/workflows/build.yml/badge.svg)](https://github.com/porturl/porturl-backend/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=porturl_porturl-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=porturl_porturl-backend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=porturl_porturl-backend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=porturl_porturl-backend)
[![Latest Release](https://img.shields.io/github/v/release/porturl/porturl-backend)](https://github.com/porturl/porturl-backend/releases/latest)

## Overview

PortUrl Backend is the central management service for the PortUrl ecosystem. Built with Spring Boot and Java 25, it provides a robust API for managing applications, categories, and user permissions.

### Key Features

- **Centralized Application Management:** Define and organize applications that should be accessible via the PortUrl dashboard.
- **Dynamic Role Management:** Automatically synchronizes role hierarchies with Keycloak for fine-grained access control.
- **Flexible Storage:** Supports both Persistent SQL (SQLite/H2) and a GitOps-friendly Transparent YAML mode with hot-reloading.
- **Observability:** Integrated with OpenTelemetry for metrics and tracing, with native support for Grafana Cloud.
- **Native Image Support:** Optimized for deployment as a GraalVM Native Image for minimal footprint and instant startup.

## Requirements

- **Keycloak:** A running Keycloak instance is **mandatory**. PortUrl uses Keycloak as its single source of truth for authentication and authorization.
- **Java 25:** Required for building and running the JAR (if not using the native binary).

## Documentation

For detailed technical information, please refer to the following guides:

- [**Technical Details**](docs/technical-details.md): Deep dive into API endpoints, storage modes, and internal workflows.
- [**Keycloak Setup**](docs/KEYCLOAK_SETUP.md): Instructions for manual Keycloak configuration.
- [**Development Guide**](docs/DEVELOPMENT.md): Information for contributors on how to build and test the backend.
- [**Architecture**](docs/ARCHITECTURE.md): Architectural overview of the backend.

## Quick Start

```bash
./gradlew bootRun
```

For production deployments, refer to the [Docker documentation](Dockerfile) or use the provided [Ansible roles](../porturl-demo-deployment-oci/ansible/).
