#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

# Run from root so Maven picks up the multi-module pom.xml (spider + webapp).
# The spider/mvnw is still available for spider-only builds.
cd "$ROOT_DIR"
exec "$ROOT_DIR/spider/mvnw" "$@"
