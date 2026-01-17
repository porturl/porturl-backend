# Changelog

## [0.3.0](https://github.com/porturl/porturl-backend/compare/v0.2.2...v0.3.0) (2026-01-17)


### Features

* add conflict in case of adding already existing category, add endpoint for batch reordering ([b7626ae](https://github.com/porturl/porturl-backend/commit/b7626aeeb0f3a6bb90eec0a60e5ca2bdd7822b67))
* add cors debug logger, fix cors config for web frontend ([c8d393e](https://github.com/porturl/porturl-backend/commit/c8d393e02e1823ada150a79b17ba5b4deacd9cc0))
* add issuer-uri to actuator/info endpoint for consumption by the android app ([f9b6023](https://github.com/porturl/porturl-backend/commit/f9b60230e8464d7a59a602d033d8ca5c98114bd3))
* add JVM docker image build and release ([2a441d5](https://github.com/porturl/porturl-backend/commit/2a441d5b9a55989bd90ef54820e6d5b8f20c45ba))
* add keycloak roles (untested, WIP) ([e5b2867](https://github.com/porturl/porturl-backend/commit/e5b28672400c867cb0b3c9e2069486cb91ad13d6))
* add sortorder per category, add category support ([5a6819c](https://github.com/porturl/porturl-backend/commit/5a6819c268ef4ff52eaf0008122761f35624c1fb))
* add swagger-ui ([200cf1b](https://github.com/porturl/porturl-backend/commit/200cf1b5c35a83aca6cf23823808c62970b55c5c))
* add user profile image support ([c97b6d6](https://github.com/porturl/porturl-backend/commit/c97b6d653f79047b52abcc8cd54b098df3a98eeb))
* allow adding/removing roles from applications ([bfa3a1d](https://github.com/porturl/porturl-backend/commit/bfa3a1d34ef4dc108a888281949ab744e5f8ab9c))
* build static binary instead of dynamically linked ([cdeddbe](https://github.com/porturl/porturl-backend/commit/cdeddbed0b223d4886de8e4dd67d60db657aaa1f))
* swagger auth support, add additional endpoints for roles, add flyway migration for users ([33d9e2c](https://github.com/porturl/porturl-backend/commit/33d9e2c40449408de19529447a29ac08fde55299))
* switch to flyway for db schema, add category and images support, enhance test data generation ([20604ff](https://github.com/porturl/porturl-backend/commit/20604ffad913ebf8647da6d265ea1cb0a2e078be))
* uploaded image cleanup job (disabled by default) ([787abcf](https://github.com/porturl/porturl-backend/commit/787abcf10706d6995e5e3718b204f47e0bdb3fd5))


### Bug Fixes

* add content-type to image endpoint ([dd36489](https://github.com/porturl/porturl-backend/commit/dd36489fef7f24a11851e7739e928ddf487c18c9))
* add missing dependency (ld: cannot find -lz) ([7782f11](https://github.com/porturl/porturl-backend/commit/7782f11f7661e8c0885b2d4fb51007ce84dfb249))
* allow all headers to prevent cors errors on adding applications through frontend ([0146f46](https://github.com/porturl/porturl-backend/commit/0146f462fd259f5e0259c42d5441c5da4050ce86))
* build mostly static executable without musl ([c53e11f](https://github.com/porturl/porturl-backend/commit/c53e11f667ce74c43269d0ff44de33dd3c38aa11))
* build native binary using compatibility arch mode ([9da00d6](https://github.com/porturl/porturl-backend/commit/9da00d6d3e008f7ee407d617950a5fcbd74574cd))
* don't cleanup user images ([cdf355d](https://github.com/porturl/porturl-backend/commit/cdf355d13c89bad669e47db4281c9e88f1c0a76a))
* flyway migrations not being applied ([8dee94e](https://github.com/porturl/porturl-backend/commit/8dee94efc81b8314923ad8a29c5fba27095bc4c0))
* image upload/retrieval works now ([5d8937c](https://github.com/porturl/porturl-backend/commit/5d8937c3bd33a24bf083f3267b3ee34b3aa859c7))
* missing endpoint for reordering, fix unit test ([e9a1dba](https://github.com/porturl/porturl-backend/commit/e9a1dba10f1c25f38445fc1e5e533ccad5df615c))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))
* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))

## [0.2.2](https://github.com/porturl/porturl-backend/compare/v0.2.1...v0.2.2) (2026-01-17)


### Bug Fixes

* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))

## [0.2.1](https://github.com/porturl/porturl-backend/compare/v0.2.0...v0.2.1) (2026-01-17)


### Bug Fixes

* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))

## [0.2.0](https://github.com/porturl/porturl-backend/compare/v0.1.0...v0.2.0) (2026-01-17)


### Features

* add conflict in case of adding already existing category, add endpoint for batch reordering ([b7626ae](https://github.com/porturl/porturl-backend/commit/b7626aeeb0f3a6bb90eec0a60e5ca2bdd7822b67))
* add cors debug logger, fix cors config for web frontend ([c8d393e](https://github.com/porturl/porturl-backend/commit/c8d393e02e1823ada150a79b17ba5b4deacd9cc0))
* add issuer-uri to actuator/info endpoint for consumption by the android app ([f9b6023](https://github.com/porturl/porturl-backend/commit/f9b60230e8464d7a59a602d033d8ca5c98114bd3))
* add JVM docker image build and release ([2a441d5](https://github.com/porturl/porturl-backend/commit/2a441d5b9a55989bd90ef54820e6d5b8f20c45ba))
* add keycloak roles (untested, WIP) ([e5b2867](https://github.com/porturl/porturl-backend/commit/e5b28672400c867cb0b3c9e2069486cb91ad13d6))
* add sortorder per category, add category support ([5a6819c](https://github.com/porturl/porturl-backend/commit/5a6819c268ef4ff52eaf0008122761f35624c1fb))
* add swagger-ui ([200cf1b](https://github.com/porturl/porturl-backend/commit/200cf1b5c35a83aca6cf23823808c62970b55c5c))
* add user profile image support ([c97b6d6](https://github.com/porturl/porturl-backend/commit/c97b6d653f79047b52abcc8cd54b098df3a98eeb))
* allow adding/removing roles from applications ([bfa3a1d](https://github.com/porturl/porturl-backend/commit/bfa3a1d34ef4dc108a888281949ab744e5f8ab9c))
* build static binary instead of dynamically linked ([cdeddbe](https://github.com/porturl/porturl-backend/commit/cdeddbed0b223d4886de8e4dd67d60db657aaa1f))
* swagger auth support, add additional endpoints for roles, add flyway migration for users ([33d9e2c](https://github.com/porturl/porturl-backend/commit/33d9e2c40449408de19529447a29ac08fde55299))
* switch to flyway for db schema, add category and images support, enhance test data generation ([20604ff](https://github.com/porturl/porturl-backend/commit/20604ffad913ebf8647da6d265ea1cb0a2e078be))
* uploaded image cleanup job (disabled by default) ([787abcf](https://github.com/porturl/porturl-backend/commit/787abcf10706d6995e5e3718b204f47e0bdb3fd5))


### Bug Fixes

* add content-type to image endpoint ([dd36489](https://github.com/porturl/porturl-backend/commit/dd36489fef7f24a11851e7739e928ddf487c18c9))
* add missing dependency (ld: cannot find -lz) ([7782f11](https://github.com/porturl/porturl-backend/commit/7782f11f7661e8c0885b2d4fb51007ce84dfb249))
* allow all headers to prevent cors errors on adding applications through frontend ([0146f46](https://github.com/porturl/porturl-backend/commit/0146f462fd259f5e0259c42d5441c5da4050ce86))
* build mostly static executable without musl ([c53e11f](https://github.com/porturl/porturl-backend/commit/c53e11f667ce74c43269d0ff44de33dd3c38aa11))
* build native binary using compatibility arch mode ([9da00d6](https://github.com/porturl/porturl-backend/commit/9da00d6d3e008f7ee407d617950a5fcbd74574cd))
* don't cleanup user images ([cdf355d](https://github.com/porturl/porturl-backend/commit/cdf355d13c89bad669e47db4281c9e88f1c0a76a))
* flyway migrations not being applied ([8dee94e](https://github.com/porturl/porturl-backend/commit/8dee94efc81b8314923ad8a29c5fba27095bc4c0))
* image upload/retrieval works now ([5d8937c](https://github.com/porturl/porturl-backend/commit/5d8937c3bd33a24bf083f3267b3ee34b3aa859c7))
* missing endpoint for reordering, fix unit test ([e9a1dba](https://github.com/porturl/porturl-backend/commit/e9a1dba10f1c25f38445fc1e5e533ccad5df615c))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))

## [0.1.0](https://github.com/porturl/porturl-backend/compare/porturl-backend-v0.0.9...porturl-backend-v0.1.0) (2026-01-17)


### Features

* add conflict in case of adding already existing category, add endpoint for batch reordering ([b7626ae](https://github.com/porturl/porturl-backend/commit/b7626aeeb0f3a6bb90eec0a60e5ca2bdd7822b67))
* add cors debug logger, fix cors config for web frontend ([c8d393e](https://github.com/porturl/porturl-backend/commit/c8d393e02e1823ada150a79b17ba5b4deacd9cc0))
* add issuer-uri to actuator/info endpoint for consumption by the android app ([f9b6023](https://github.com/porturl/porturl-backend/commit/f9b60230e8464d7a59a602d033d8ca5c98114bd3))
* add JVM docker image build and release ([2a441d5](https://github.com/porturl/porturl-backend/commit/2a441d5b9a55989bd90ef54820e6d5b8f20c45ba))
* add keycloak roles (untested, WIP) ([e5b2867](https://github.com/porturl/porturl-backend/commit/e5b28672400c867cb0b3c9e2069486cb91ad13d6))
* add sortorder per category, add category support ([5a6819c](https://github.com/porturl/porturl-backend/commit/5a6819c268ef4ff52eaf0008122761f35624c1fb))
* add swagger-ui ([200cf1b](https://github.com/porturl/porturl-backend/commit/200cf1b5c35a83aca6cf23823808c62970b55c5c))
* add user profile image support ([c97b6d6](https://github.com/porturl/porturl-backend/commit/c97b6d653f79047b52abcc8cd54b098df3a98eeb))
* allow adding/removing roles from applications ([bfa3a1d](https://github.com/porturl/porturl-backend/commit/bfa3a1d34ef4dc108a888281949ab744e5f8ab9c))
* build static binary instead of dynamically linked ([cdeddbe](https://github.com/porturl/porturl-backend/commit/cdeddbed0b223d4886de8e4dd67d60db657aaa1f))
* swagger auth support, add additional endpoints for roles, add flyway migration for users ([33d9e2c](https://github.com/porturl/porturl-backend/commit/33d9e2c40449408de19529447a29ac08fde55299))
* switch to flyway for db schema, add category and images support, enhance test data generation ([20604ff](https://github.com/porturl/porturl-backend/commit/20604ffad913ebf8647da6d265ea1cb0a2e078be))
* uploaded image cleanup job (disabled by default) ([787abcf](https://github.com/porturl/porturl-backend/commit/787abcf10706d6995e5e3718b204f47e0bdb3fd5))


### Bug Fixes

* add content-type to image endpoint ([dd36489](https://github.com/porturl/porturl-backend/commit/dd36489fef7f24a11851e7739e928ddf487c18c9))
* add missing dependency (ld: cannot find -lz) ([7782f11](https://github.com/porturl/porturl-backend/commit/7782f11f7661e8c0885b2d4fb51007ce84dfb249))
* allow all headers to prevent cors errors on adding applications through frontend ([0146f46](https://github.com/porturl/porturl-backend/commit/0146f462fd259f5e0259c42d5441c5da4050ce86))
* build mostly static executable without musl ([c53e11f](https://github.com/porturl/porturl-backend/commit/c53e11f667ce74c43269d0ff44de33dd3c38aa11))
* build native binary using compatibility arch mode ([9da00d6](https://github.com/porturl/porturl-backend/commit/9da00d6d3e008f7ee407d617950a5fcbd74574cd))
* don't cleanup user images ([cdf355d](https://github.com/porturl/porturl-backend/commit/cdf355d13c89bad669e47db4281c9e88f1c0a76a))
* flyway migrations not being applied ([8dee94e](https://github.com/porturl/porturl-backend/commit/8dee94efc81b8314923ad8a29c5fba27095bc4c0))
* image upload/retrieval works now ([5d8937c](https://github.com/porturl/porturl-backend/commit/5d8937c3bd33a24bf083f3267b3ee34b3aa859c7))
* missing endpoint for reordering, fix unit test ([e9a1dba](https://github.com/porturl/porturl-backend/commit/e9a1dba10f1c25f38445fc1e5e533ccad5df615c))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
