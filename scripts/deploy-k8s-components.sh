#! /bin/bash
set -ex

: ${DOCKER_REPOSITORY?"You must export DOCKER_REPOSITORY"}
: ${IMAGE_TAG?"You must export IMAGE_TAG"}

ROOT_DIR=$(dirname $0)/..
NAMESPACE=idracdemo
ELK_ENABLED=false

if [ $ELK_ENABLED == "true" ] ; then
    helm upgrade --install --wait \
    ${NAMESPACE}-elastic-stack \
    --namespace ${NAMESPACE} \
    charts/elastic-stack
fi

helm upgrade --install  --wait \
${NAMESPACE}-analytic \
--namespace ${NAMESPACE} \
${ROOT_DIR}/charts/analytic \
--set image.repository=${DOCKER_REPOSITORY} \
--set image.tag=${IMAGE_TAG}
