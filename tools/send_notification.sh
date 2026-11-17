#!/usr/bin/env bash
#
# Sends one of the sample payloads in tools/payloads/ to a device using the
# FCM HTTP v1 API.
#
# Usage:
#   ./tools/send_notification.sh <firebase-project-id> <device-token> <payload.json>
#
# Example:
#   ./tools/send_notification.sh my-project-id "eXaMpLe:APA91b..." tools/payloads/01_sample.json
#
# Authentication (pick one):
#   - Set ACCESS_TOKEN yourself, or
#   - Have the gcloud CLI signed in with an account that can send FCM messages:
#       gcloud auth activate-service-account --key-file=/path/to/service-account.json
#     (Never commit the service-account key.)
#
# Get <device-token> from the app: Inbox → ⋮ → "Copy FCM token".

set -euo pipefail

if [[ $# -ne 3 ]]; then
  sed -n '3,20p' "$0"
  exit 1
fi

PROJECT_ID="$1"
DEVICE_TOKEN="$2"
PAYLOAD_FILE="$3"

if [[ ! -f "$PAYLOAD_FILE" ]]; then
  echo "Payload file not found: $PAYLOAD_FILE" >&2
  exit 1
fi

ACCESS_TOKEN="${ACCESS_TOKEN:-$(gcloud auth print-access-token --scopes=https://www.googleapis.com/auth/firebase.messaging 2>/dev/null || gcloud auth print-access-token)}"

BODY="$(sed "s|DEVICE_TOKEN|${DEVICE_TOKEN}|" "$PAYLOAD_FILE")"

curl --silent --show-error --fail-with-body \
  -X POST "https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d "${BODY}"
echo
