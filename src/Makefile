SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

DOCKER_REPOSITORY ?= "edeckers"
DOCKER_IMAGE ?= "ktsmtp"
DOCKER_TAG ?= "1.0.0"

all: help

build: clean
	bin/run-build-service

build-container: build
	DOCKER_REPOSITORY=$(DOCKER_REPOSITORY) \
	DOCKER_IMAGE=$(DOCKER_IMAGE) \
	DOCKER_TAG=$(DOCKER_TAG) \
	bin/run-build-container

ci:
	bin/run-ci

clean:
	bin/run-clean

deploy-container:
	DOCKER_REPOSITORY=$(DOCKER_REPOSITORY) \
	DOCKER_IMAGE=$(DOCKER_IMAGE) \
	DOCKER_TAG=$(DOCKER_TAG) \
	bin/run-deploy-container

help:
	bin/print-help

run: build
	bin/run-service

test: build
	bin/run-tests

.PHONY: build build-container ci clean deploy-container help run test
