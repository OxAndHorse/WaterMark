#!/bin/bash
IMG=data-watermark-service
TAG=0.1
docker build -t ${IMG}:${TAG} .
docker build -f Dockerfile_mysql . -t mysql:dws

# docker save ${IMG}:${TAG} | gzip > ${IMG}_${TAG}.tar.gz
