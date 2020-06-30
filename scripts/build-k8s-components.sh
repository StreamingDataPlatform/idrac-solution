#! /bin/bash
set -ex

: ${DOCKER_REPOSITORY?"You must export DOCKER_REPOSITORY"}
: ${IMAGE_TAG?"You must export IMAGE_TAG"}

ROOT_DIR=$(dirname $0)/..

# Build components
docker build -f ${ROOT_DIR}/gateway/Dockerfile ${ROOT_DIR} --tag ${DOCKER_REPOSITORY}/gateway:${IMAGE_TAG}

# Build grafana and InfluxDB components
docker build -f ${ROOT_DIR}/docker/influxdb/Dockerfile ${ROOT_DIR} --tag ${DOCKER_REPOSITORY}/influxdb:${IMAGE_TAG}
docker build -f ${ROOT_DIR}/docker/grafana/Dockerfile ${ROOT_DIR} --tag ${DOCKER_REPOSITORY}/grafana:${IMAGE_TAG}

docker push ${DOCKER_REPOSITORY}/gateway:${IMAGE_TAG}

# push grafana and InfluxDB components
docker push ${DOCKER_REPOSITORY}/influxdb:${IMAGE_TAG}
docker push ${DOCKER_REPOSITORY}/grafana:${IMAGE_TAG}
