#!/usr/bin/env bash
#
# Copyright (c) 2026 jqwik team
# Copyright (c) 2026 Adeptum AB and Adam Waldenberg
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Cuts a release: builds the module at a fixed version, keeps the jar in
# releases/ under that version, records it in a commit and a tag, and opens
# the next snapshot.
#
# Run ./create-release.sh --help for what it takes.

set -euo pipefail

readonly ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly BUILD_FILE="build.gradle.kts"
readonly README="README.md"
readonly RELEASES="releases"
readonly VERSION_PROPERTY="jqwikArquillianVersion"

die() { printf '%s\n' "$*" >&2; exit 1; }
step() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

usage() {
	cat <<'USAGE'
Cuts a release: builds the module at a fixed version, keeps the jar in
releases/ under that version, records it in a commit and a tag,
and reopens the next revision as a snapshot. Nothing is pushed and nothing is
published.

  ./create-release.sh [options]

The version released is the one the build file is already working towards,
with the snapshot suffix dropped; it is shown and confirmed before anything
is built. What is opened afterwards is up to --bump.

  --bump=revrevision the third number, and the default: 0.1.0 opens
                     0.1.1-SNAPSHOT.
  --bump=revision    the second, zeroing the third: 0.1.0 opens
                     0.2.0-SNAPSHOT.
  --bump=version     the first, zeroing the rest: 0.1.0 opens
                     1.0.0-SNAPSHOT.

  --skip-tests       build only. Nothing is verified, and no Payara server
                     is started.
  --help, -h         this.
USAGE
}

tests="all"
bump="revrevision"
for argument in "$@"; do
	case "$argument" in
		--help|-h) usage; exit 0 ;;
		--skip-tests) tests="none" ;;
		--bump=version|--bump=revision|--bump=revrevision) bump="${argument#--bump=}" ;;
		--bump=*) die "Bump one of version, revision or revrevision, not '${argument#--bump=}'." ;;
		*) usage >&2; die "
Takes no version: the build file already says which one comes next.
Unexpected argument '$argument'." ;;
	esac
done

is_version() { [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; }

bumped() {
	local major="${1%%.*}" rest="${1#*.}"
	local minor="${rest%%.*}" patch="${rest#*.}"
	case "$2" in
		version) printf '%s.0.0' "$(( major + 1 ))" ;;
		revision) printf '%s.%s.0' "$major" "$(( minor + 1 ))" ;;
		*) printf '%s.%s.%s' "$major" "$minor" "$(( patch + 1 ))" ;;
	esac
}

build_version() {
	sed -n "s/^val $VERSION_PROPERTY = \"\(.*\)\"$/\1/p" "$BUILD_FILE"
}

set_build_version() {
	sed -i "s/^val $VERSION_PROPERTY = \".*\"$/val $VERSION_PROPERTY = \"$1\"/" "$BUILD_FILE"
	[ "$(build_version)" = "$1" ] || die "$BUILD_FILE did not take version $1."
}

# The readme tells users which version to depend on, so it follows releases only
# and keeps naming the last one while the next snapshot is open.
set_readme_version() {
	local shown
	shown="$(grep -oE "jqwik-arquillian:[0-9][^\"]*" "$README" | head -1 | cut -d: -f2)"
	[ -n "$shown" ] || die "$README names no jqwik-arquillian version to replace."
	sed -i "s/${shown//./\\.}/$1/g" "$README"
}

cd "$ROOT"

git rev-parse --git-dir >/dev/null 2>&1 || die "Not a git repository."

dirty="$(git status --porcelain)"
[ -z "$dirty" ] || die "Working tree is not clean; commit or stash first:
$dirty"

snapshot="$(build_version)"
version="${snapshot%-SNAPSHOT}"
is_version "$version" \
	|| die "$BUILD_FILE reads '$snapshot', which names no version to release.
Set $VERSION_PROPERTY to a major.minor.patch snapshot first."
next="$(bumped "$version" "$bump")-SNAPSHOT"

# Tag names and commit subjects follow the other jqwik modules
tag="$version"
git rev-parse -q --verify "refs/tags/$tag" >/dev/null \
	&& die "Tag $tag already exists; that release has been cut."

jar="build/libs/jqwik-arquillian-$version.jar"
artifact="$RELEASES/jqwik-arquillian-$version.jar"
[ -e "$artifact" ] && die "$artifact already exists."

printf 'Release %s, then open %s?' "$version" "$next"
if [ -t 0 ]; then
	printf ' [Y/n] '
	read -r answer
	case "$answer" in
		""|y|Y|yes) ;;
		*) die "Nothing released." ;;
	esac
else
	printf '\n'
fi

step "Setting the version to $version"
set_build_version "$version"
set_readme_version "$version"

restore_snapshot() {
	git checkout -- "$BUILD_FILE" "$README" 2>/dev/null || true
}
trap restore_snapshot ERR

case "$tests" in
	all)
		step "Building and testing"
		./gradlew clean check jar
		;;
	none)
		step "Building, nothing verified"
		./gradlew clean jar
		;;
esac

trap - ERR

[ -f "$jar" ] || die "The build produced no $jar."

step "Keeping the jar"
mkdir -p "$RELEASES"
cp "$jar" "$artifact"
printf '%s (%s)\n' "$(basename "$artifact")" "$(du -h "$artifact" | cut -f1)"

step "Recording the release"
git add "$BUILD_FILE" "$README" "$artifact"
git commit -q -m "Set release version $version"
git tag "$tag"

step "Opening the next snapshot"
set_build_version "$next"
git add "$BUILD_FILE"
git commit -q -m "Advance version to $next"

printf '\n\033[1mReleased %s\033[0m\n' "$version"
printf '  jar    %s\n' "$artifact"
printf '  tag    %s\n' "$tag"
printf '  next   %s\n' "$next"
printf '\nNothing was pushed and nothing was published.\n'
