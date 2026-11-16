#!/usr/bin/env bash
set -euo pipefail

cd /src

: "${OIDC_BASE_URL:?OIDC_BASE_URL manquante}"

git -C /home/opam/opam-repository pull --quiet origin master
opam update --yes
(cd cli && opam install . --deps-only --with-test --yes)
pnpm --dir cli install --frozen-lockfile --ignore-workspace

pnpm install --frozen-lockfile
pnpm exec gulp build
pnpm cljs:release-electron
pnpm db-worker-node:bundle
opam exec -- pnpm cli:release
pnpm webpack-app-build
pnpm desktop:prepare-runtime-js

cd static
: > pnpm-workspace.yaml
pnpm install --frozen-lockfile
pnpm electron:make
