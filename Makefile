SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

all: help

build: clean
	bin/run-build

ci:
	bin/run-ci

clean:
	bin/run-clean

help:
	bin/print-help

run: build
	bin/run-service

test: build
	bin/run-tests

.PHONY: build ci clean help run test
