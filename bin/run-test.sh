#!/usr/bin/env bash

PORT=${PORT:-9999}
DOMAIN=${DOMAIN:-"deckers.io"}
HOST=${HOST:-"localhost"}

echo "Connecting to ${HOST}:${PORT}"

function slowcat(){ while read; do sleep .05; echo "$REPLY"; done; }

echo "helo ${DOMAIN}
mail from: ely@${DOMAIN}
rcpt to: ely@deckers.io
data
Subject: Testtest
From: ely@${DOMAIN}
Date: 2021-05-9
hello world

.
quit
" | slowcat  | nc ${HOST} ${PORT}

