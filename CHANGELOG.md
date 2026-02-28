# Changelog

## [0.11.0](https://github.com/porturl/porturl-backend/compare/v0.10.0...v0.11.0) (2026-02-28)


### Features

* add flag to fully disable (default) otel logs/traces/metrics collection ([01bdc7c](https://github.com/porturl/porturl-backend/commit/01bdc7cb9eeb249f951b87ff07a32e3c39eb5ff1))
* add full docker compose for local development (untested) ([bcea8d8](https://github.com/porturl/porturl-backend/commit/bcea8d8676927e0c36f678ae1753a379d36fe93f))
* add small script to add adb portforwarding ([3faca65](https://github.com/porturl/porturl-backend/commit/3faca65cb2d1632813ea7da281e58c1e24357c19))
* Add support for linking external Keycloak Clients and Cross-Realm roles ([0edbd3c](https://github.com/porturl/porturl-backend/commit/0edbd3c53366a9ee4f9d7d4f13f613066a559d75))
* add support for opentelemetry tracing (untested) ([6954b64](https://github.com/porturl/porturl-backend/commit/6954b6411e7c6982cb2ef8a9054a02d3492f708f))
* add yaml backend which can be used for infrastruture as code (kubernetes configmap/puppet/...), editing within the app save the changes back to yaml but the admin has to make sure those changes are persisted otherwise they will be overwritten by puppet/argocd/... ([d975a0b](https://github.com/porturl/porturl-backend/commit/d975a0bf23a0a59df84d5a575a9ebf31924d6fb0))
* allow listing realms and filter everything internal from keycloak/porturl ([9a5ea6f](https://github.com/porturl/porturl-backend/commit/9a5ea6fd43a647b879fbddc7da7b6cf35e39476d))
* import/export of apps and categories ([db49bac](https://github.com/porturl/porturl-backend/commit/db49bacb790e9ce0e6b84b76feace83d69e6b4e3))
* listview and alphabetical sorting as setting ([661ed6a](https://github.com/porturl/porturl-backend/commit/661ed6a64874fc59e2a0e0e1699a21136731e8d0))
* manage roles in clients, add cross realm/client management, use dedicated client in master realm for cross realm management, update deps ([9979b05](https://github.com/porturl/porturl-backend/commit/9979b05957efc018907a5d3b613f63b9f9e5c372))
* use isolated chrome session with sso bridge ([745b70a](https://github.com/porturl/porturl-backend/commit/745b70ab49f64b03d3ab1651ee2b6de8437a5ba4))


### Bug Fixes

* broken images due to traefik proxy https rewrite ([2d4e8ad](https://github.com/porturl/porturl-backend/commit/2d4e8addefcd6b463bf22f4e53440b78eb68bf26))
* default sortorder alphabetical ([9440706](https://github.com/porturl/porturl-backend/commit/94407064744537ad2f47e3d66e8d88d0282a030b))
* disabling telemetry actually works now ([a1d75a3](https://github.com/porturl/porturl-backend/commit/a1d75a3b08dae4e8dd3b09d84a466833d40a3a28))
* first login not showing apps/categories due to race condition in user creation logic ([982d351](https://github.com/porturl/porturl-backend/commit/982d351da01da4f5a30c92dc0c43fc6c75afc9a4))
* improve otel timeout strategy to only poll every 30seconds if endpoint is up ([1a850e2](https://github.com/porturl/porturl-backend/commit/1a850e20ad1f145e97cd894fa0c7858f01e1780c))
* opentelemetry collection of logs, metrics and traces now actually works ([752d397](https://github.com/porturl/porturl-backend/commit/752d3972eb02137fea5d5bbf25edd723fa240ad1))
* remove unused category enable/icon fields ([682ce61](https://github.com/porturl/porturl-backend/commit/682ce612460cc78d729bba58f9419ea84062d6da))
* repair broken profile image upload ([3c8a3aa](https://github.com/porturl/porturl-backend/commit/3c8a3aacc3e9b7346951153510eba29485395eae))
* simplify icons (only one size) and fix setting on app creation ([f934ac5](https://github.com/porturl/porturl-backend/commit/f934ac58ab97f02b9df5b5b48e60b0034df59e87))
* startup works now without telemetry configured ([bca8172](https://github.com/porturl/porturl-backend/commit/bca81729e9832860ac41bd04160a6741f6ff3d5d))
* tests ([95fb1f2](https://github.com/porturl/porturl-backend/commit/95fb1f21256e009bbe997204c28db3e62aeb4b9b))
* update testdata script for removed category enable/icon fields, simplified icons for applications ([bc4d5f1](https://github.com/porturl/porturl-backend/commit/bc4d5f19e242e6d7d173c0e43a8c935acd07cb88))
* use debug loglevel instead of info for imagecleanup ([170216d](https://github.com/porturl/porturl-backend/commit/170216d0c156967d56b9ab2b8c4695258ad41b14))
* workaround for github.com/googleapis/release-please/issues/1650 ([7a47d65](https://github.com/porturl/porturl-backend/commit/7a47d65a066cf822b1601fed05e9887e39890fa7))


### Documentation

* update keycloak setup docs and sample realms ([7b66ed5](https://github.com/porturl/porturl-backend/commit/7b66ed59c908bb6323d14832ef2fac870e4a4e68))

## [0.10.0](https://github.com/porturl/porturl-backend/compare/v0.9.2...v0.10.0) (2026-01-18)


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
* add missing permissions ([bac7576](https://github.com/porturl/porturl-backend/commit/bac7576646f782adc76e7b63e060a0ba06be1b12))
* allow all headers to prevent cors errors on adding applications through frontend ([0146f46](https://github.com/porturl/porturl-backend/commit/0146f462fd259f5e0259c42d5441c5da4050ce86))
* build mostly static executable without musl ([c53e11f](https://github.com/porturl/porturl-backend/commit/c53e11f667ce74c43269d0ff44de33dd3c38aa11))
* build native binary using compatibility arch mode ([9da00d6](https://github.com/porturl/porturl-backend/commit/9da00d6d3e008f7ee407d617950a5fcbd74574cd))
* do the tagging immediately to prevent wrong release-please pr changelogs ([e6db721](https://github.com/porturl/porturl-backend/commit/e6db721c6c915e0604a3532c185290e7e220521b))
* do the tagging immediately to prevent wrong release-please pr changelogs ([d46b079](https://github.com/porturl/porturl-backend/commit/d46b0797bf752f797119f85c3f07e4f8bbeb3113))
* docker step fails due to missing tag ([2bc02a4](https://github.com/porturl/porturl-backend/commit/2bc02a4729703f8e2249920a844f2b85a4aa613b))
* don't cleanup user images ([cdf355d](https://github.com/porturl/porturl-backend/commit/cdf355d13c89bad669e47db4281c9e88f1c0a76a))
* flyway migrations not being applied ([8dee94e](https://github.com/porturl/porturl-backend/commit/8dee94efc81b8314923ad8a29c5fba27095bc4c0))
* image upload/retrieval works now ([5d8937c](https://github.com/porturl/porturl-backend/commit/5d8937c3bd33a24bf083f3267b3ee34b3aa859c7))
* label must exist first ([81edffd](https://github.com/porturl/porturl-backend/commit/81edffd4097d627e3f99df5d5c3f9993df38dd98))
* missing endpoint for reordering, fix unit test ([e9a1dba](https://github.com/porturl/porturl-backend/commit/e9a1dba10f1c25f38445fc1e5e533ccad5df615c))
* move x-release-please-version to line above to prevent problems on wrong interpretation ([a478267](https://github.com/porturl/porturl-backend/commit/a478267e5ed6f40f631566143e8f9f4c4386bf05))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* readd missing github release information ([03def02](https://github.com/porturl/porturl-backend/commit/03def0248644cef1fc51c7c84f7eb2224488e665))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))
* use separate step for finishing release ([b8e721c](https://github.com/porturl/porturl-backend/commit/b8e721ca42f3d63cb64fc3424c8b5f879f6356f1))
* use start and end markers for release please version ([49ee08a](https://github.com/porturl/porturl-backend/commit/49ee08a3b5a96848918fbc5abc346eb89e40f208))
* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))
* wrong flag for finding release pr ([19e5c2a](https://github.com/porturl/porturl-backend/commit/19e5c2a4900a543e45bf052e67c6a27e1733fc03))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))
* wrong indentation ([48e93b7](https://github.com/porturl/porturl-backend/commit/48e93b7435cac20dd9ebe8b0a6680ed0b1ad42f5))

## [0.9.2](https://github.com/porturl/porturl-backend/compare/v0.9.1...v0.9.2) (2026-01-18)


### Bug Fixes

* do the tagging immediately to prevent wrong release-please pr changelogs ([d46b079](https://github.com/porturl/porturl-backend/commit/d46b0797bf752f797119f85c3f07e4f8bbeb3113))

## [0.9.1](https://github.com/porturl/porturl-backend/compare/v0.9.0...v0.9.1) (2026-01-18)


### Bug Fixes

* add missing permissions ([bac7576](https://github.com/porturl/porturl-backend/commit/bac7576646f782adc76e7b63e060a0ba06be1b12))
* readd missing github release information ([03def02](https://github.com/porturl/porturl-backend/commit/03def0248644cef1fc51c7c84f7eb2224488e665))

## [0.9.0](https://github.com/porturl/porturl-backend/compare/v0.8.0...v0.9.0) (2026-01-18)


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
* docker step fails due to missing tag ([2bc02a4](https://github.com/porturl/porturl-backend/commit/2bc02a4729703f8e2249920a844f2b85a4aa613b))
* don't cleanup user images ([cdf355d](https://github.com/porturl/porturl-backend/commit/cdf355d13c89bad669e47db4281c9e88f1c0a76a))
* flyway migrations not being applied ([8dee94e](https://github.com/porturl/porturl-backend/commit/8dee94efc81b8314923ad8a29c5fba27095bc4c0))
* image upload/retrieval works now ([5d8937c](https://github.com/porturl/porturl-backend/commit/5d8937c3bd33a24bf083f3267b3ee34b3aa859c7))
* label must exist first ([81edffd](https://github.com/porturl/porturl-backend/commit/81edffd4097d627e3f99df5d5c3f9993df38dd98))
* missing endpoint for reordering, fix unit test ([e9a1dba](https://github.com/porturl/porturl-backend/commit/e9a1dba10f1c25f38445fc1e5e533ccad5df615c))
* move x-release-please-version to line above to prevent problems on wrong interpretation ([a478267](https://github.com/porturl/porturl-backend/commit/a478267e5ed6f40f631566143e8f9f4c4386bf05))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))
* use separate step for finishing release ([b8e721c](https://github.com/porturl/porturl-backend/commit/b8e721ca42f3d63cb64fc3424c8b5f879f6356f1))
* use start and end markers for release please version ([49ee08a](https://github.com/porturl/porturl-backend/commit/49ee08a3b5a96848918fbc5abc346eb89e40f208))
* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))
* wrong flag for finding release pr ([19e5c2a](https://github.com/porturl/porturl-backend/commit/19e5c2a4900a543e45bf052e67c6a27e1733fc03))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))
* wrong indentation ([48e93b7](https://github.com/porturl/porturl-backend/commit/48e93b7435cac20dd9ebe8b0a6680ed0b1ad42f5))

## [0.8.0](https://github.com/porturl/porturl-backend/compare/v0.7.1...v0.8.0) (2026-01-18)


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
* label must exist first ([81edffd](https://github.com/porturl/porturl-backend/commit/81edffd4097d627e3f99df5d5c3f9993df38dd98))
* missing endpoint for reordering, fix unit test ([e9a1dba](https://github.com/porturl/porturl-backend/commit/e9a1dba10f1c25f38445fc1e5e533ccad5df615c))
* move x-release-please-version to line above to prevent problems on wrong interpretation ([a478267](https://github.com/porturl/porturl-backend/commit/a478267e5ed6f40f631566143e8f9f4c4386bf05))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))
* use separate step for finishing release ([b8e721c](https://github.com/porturl/porturl-backend/commit/b8e721ca42f3d63cb64fc3424c8b5f879f6356f1))
* use start and end markers for release please version ([49ee08a](https://github.com/porturl/porturl-backend/commit/49ee08a3b5a96848918fbc5abc346eb89e40f208))
* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))
* wrong flag for finding release pr ([19e5c2a](https://github.com/porturl/porturl-backend/commit/19e5c2a4900a543e45bf052e67c6a27e1733fc03))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))
* wrong indentation ([48e93b7](https://github.com/porturl/porturl-backend/commit/48e93b7435cac20dd9ebe8b0a6680ed0b1ad42f5))

## [0.7.1](https://github.com/porturl/porturl-backend/compare/v0.7.0...v0.7.1) (2026-01-18)


### Bug Fixes

* use separate step for finishing release ([b8e721c](https://github.com/porturl/porturl-backend/commit/b8e721ca42f3d63cb64fc3424c8b5f879f6356f1))

## [0.7.0](https://github.com/porturl/porturl-backend/compare/v0.6.0...v0.7.0) (2026-01-18)


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
* label must exist first ([81edffd](https://github.com/porturl/porturl-backend/commit/81edffd4097d627e3f99df5d5c3f9993df38dd98))
* missing endpoint for reordering, fix unit test ([e9a1dba](https://github.com/porturl/porturl-backend/commit/e9a1dba10f1c25f38445fc1e5e533ccad5df615c))
* move x-release-please-version to line above to prevent problems on wrong interpretation ([a478267](https://github.com/porturl/porturl-backend/commit/a478267e5ed6f40f631566143e8f9f4c4386bf05))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))
* use start and end markers for release please version ([49ee08a](https://github.com/porturl/porturl-backend/commit/49ee08a3b5a96848918fbc5abc346eb89e40f208))
* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))
* wrong flag for finding release pr ([19e5c2a](https://github.com/porturl/porturl-backend/commit/19e5c2a4900a543e45bf052e67c6a27e1733fc03))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))

## [0.6.0](https://github.com/porturl/porturl-backend/compare/v0.5.0...v0.6.0) (2026-01-18)


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
* move x-release-please-version to line above to prevent problems on wrong interpretation ([a478267](https://github.com/porturl/porturl-backend/commit/a478267e5ed6f40f631566143e8f9f4c4386bf05))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))
* use start and end markers for release please version ([49ee08a](https://github.com/porturl/porturl-backend/commit/49ee08a3b5a96848918fbc5abc346eb89e40f208))
* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))
* wrong flag for finding release pr ([19e5c2a](https://github.com/porturl/porturl-backend/commit/19e5c2a4900a543e45bf052e67c6a27e1733fc03))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))

## [0.5.0](https://github.com/porturl/porturl-backend/compare/v0.4.0...v0.5.0) (2026-01-18)


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
* move x-release-please-version to line above to prevent problems on wrong interpretation ([a478267](https://github.com/porturl/porturl-backend/commit/a478267e5ed6f40f631566143e8f9f4c4386bf05))
* read cors settings from application.yaml ([16de912](https://github.com/porturl/porturl-backend/commit/16de912a726c42ff28e99a1d2a1d865ff781f4a2))
* readd missing endpoint ([3ea72dc](https://github.com/porturl/porturl-backend/commit/3ea72dcf2253cbb6d997ddcfa1a3f118a92c3e7d))
* remove data-rest as it exposes the entities via rest api directly ([c22776a](https://github.com/porturl/porturl-backend/commit/c22776a9d60eeb110592692cf9c28123108dcd85))
* remove permitAll as this matches first before cors configuration ([1f5760a](https://github.com/porturl/porturl-backend/commit/1f5760af4e65ede2337f7778e7d1b852d2ca55c1))
* revert missing code ([cba55c8](https://github.com/porturl/porturl-backend/commit/cba55c8597970c7874cc1726f4183aae9cfa47ae))
* use draft release until artifacts have been published, use personal access token ([fc35520](https://github.com/porturl/porturl-backend/commit/fc35520224d98109bd5312a13c5e0e85cae52bc9))
* use start and end markers for release please version ([49ee08a](https://github.com/porturl/porturl-backend/commit/49ee08a3b5a96848918fbc5abc346eb89e40f208))
* very important docs fix ([2f3e5f9](https://github.com/porturl/porturl-backend/commit/2f3e5f9d6161a5c296b1ce3154aa5cc4ef376a63))
* wrong flag for finding release pr ([19e5c2a](https://github.com/porturl/porturl-backend/commit/19e5c2a4900a543e45bf052e67c6a27e1733fc03))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))

## [0.4.0](https://github.com/porturl/porturl-backend/compare/v0.3.0...v0.4.0) (2026-01-17)


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
* wrong flag for finding release pr ([19e5c2a](https://github.com/porturl/porturl-backend/commit/19e5c2a4900a543e45bf052e67c6a27e1733fc03))
* wrong flag for finding release pr ([92cd0a4](https://github.com/porturl/porturl-backend/commit/92cd0a4dd0af9499b7e2b0afd0cc84a3891e5600))

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
