# Automated Release Process

This document describes the automated release strategy for the PortURL Backend, focusing on how we handle production bugfixes and OS security updates without manual intervention.

## 1. Branching Strategy

We utilize a two-stream branching model to isolate development from production patches:

-   **`main` Branch**: The primary branch for new features and minor/major dependency updates. Releases from here trigger a "reset" of the production stream.
-   **`production` Branch**: A rolling branch that always points to the latest released tag. It is the target for automated bugfixes and security patches.

## 2. Dependency & OS Updates

### Java Dependencies
`Dependabot` monitors `build.gradle.kts` on both `main` and `production` branches. 
- Updates on `main` follow the standard PR review process.
- **Bugfix/Patch updates** on `production` are prioritized for automation.

### Docker Base Images (OS Patches)
We use Google's **Distroless** base images (`gcr.io/distroless/...`). Because these images lack a package manager (`yum`/`apt`), we receive OS-level security updates by updating the base image reference. 
`Dependabot` monitors these references in the `Dockerfile` and `Dockerfile.native` on the `production` branch.

## 3. The Automated Bugfix Loop ("Zero-Touch" Patching)

To ensure production stays secure without pulling in unreleased features from `main`, we use the following automated loop:

1.  **Detection**: `Dependabot` identifies a bugfix (Java library) or a new Distroless base image (OS patch) and opens a PR against the `production` branch.
2.  **Verification**: GitHub Actions triggers the full test suite, including **Keycloak Integration Tests** using Testcontainers.
3.  **Auto-Merge**: If the tests pass, the PR is automatically merged into `production`.
4.  **Release Preparation**: `release-please` detects the merge on `production` and opens a **Release PR** (e.g., bumping `v0.4.1` to `v0.4.1.1`).
5.  **Final Validation**: Auto-merge is enabled on the Release PR. Once the integration tests pass again on this version-bump commit, the PR merges itself.
6.  **Publication**: A new GitHub Release is created, and the `publish-release.yml` workflow builds and pushes the updated JVM and Native Docker images to the registry.

## 4. Feature Releases from `main`

When a Feature Release is merged on `main`:
1.  `release-please` cuts the new version (e.g., `v0.5.0`).
2.  A specialized step in the workflow **force-pushes** the `production` branch to this new tag.
3.  The `production` branch is now "reset" and ready to receive bugfixes for the `v0.5.x` stream.

## 5. Security Note

All automated PRs are created using a `RELEASE_TOKEN` (Personal Access Token). This is required because the default `GITHUB_TOKEN` cannot trigger subsequent GitHub Actions (like our integration tests), which would stall the automation loop.
