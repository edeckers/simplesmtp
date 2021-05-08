#!/usr/bin/env bash

PORT=${PORT:-9999}

echo "ehlo domain.com
helo domain.com
mail from: mailbox@domain.com
rcpt to: ely@infi.nl
data
hello world

.

quit" | nc localhost ${PORT}

