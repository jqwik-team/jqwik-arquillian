#!/usr/bin/env bash
#
# Copyright (c) 2025-2026 jqwik team
# Copyright (c) 2025-2026 Adeptum AB and Adam Waldenberg
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Hands a release uploaded to the OSSRH staging API over to
# central.sonatype.com, where it shows up as a deployment. ./gradlew publish
# runs it after the upload; run it by hand if that step failed.
#
# The token travels in the environment so that it stays out of the process
# list.

set -euo pipefail

readonly NAMESPACE="net.jqwik"
token="${PORTAL_TOKEN:?Set PORTAL_TOKEN to the base64 encoded portal token.}"

echo "Posting new deployment to ossrh-staging-api.central"
curl --fail-with-body -X 'POST' \
	"https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$NAMESPACE" \
	-H 'accept: application/json' \
	-H "Authorization: Bearer $token"
echo
