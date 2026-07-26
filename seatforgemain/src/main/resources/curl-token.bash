#!/usr/bin/env bash

set -euo pipefail


: "${AUTH0_ORGANIZER_CLIENT_ID:?Define AUTH0_ORGANIZER_CLIENT_ID}"
: "${AUTH0_ORGANIZER_CLIENT_SECRET:?Define AUTH0_ORGANIZER_CLIENT_SECRET}"
: "${AUTH0_BUYER_CLIENT_ID:?Define AUTH0_BUYER_CLIENT_ID}"
: "${AUTH0_BUYER_CLIENT_SECRET:?Define AUTH0_BUYER_CLIENT_SECRET}"
# Estos valores no son secretos y pueden sobrescribirse desde el entorno.
AUTH0_ISSUER="${AUTH0_ISSUER:-https://dev-b2bnjwq3sll2xfxh.us.auth0.com/}"
AUTH0_AUDIENCE="${AUTH0_AUDIENCE:-https://api.seatforge.local}"

AUTH0_TOKEN_URL="${https://dev-b2bnjwq3sll2xfxh.us.auth0.com}/oauth/token"

echo "Solicitando token de ORGANIZER..."
curl --fail-with-body --silent --show-error \
  --request POST \
  --url "https://dev-b2bnjwq3sll2xfxh.us.auth0.com/oauth/token" \
  --header "content-type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=${AUTH0_ORGANIZER_CLIENT_ID}" \
  --data-urlencode "client_secret=${AUTH0_ORGANIZER_CLIENT_SECRET}" \
  --data-urlencode "audience=https://api.seatforge.local" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "scope=create:events publish:events reserve:tickets create:orders read:orders pay:orders"

printf '\n\n'

echo "Solicitando token de BUYER..."
curl --fail-with-body --silent --show-error \
  --request POST \
  --url "${AUTH0_TOKEN_URL}" \
  --header "content-type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=${AUTH0_BUYER_CLIENT_ID}" \
  --data-urlencode "client_secret=${AUTH0_BUYER_CLIENT_SECRET}" \
    --data-urlencode "audience=${AUTH0_AUDIENCE}" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "scope=reserve:tickets create:orders read:orders pay:orders"

printf '\n'
