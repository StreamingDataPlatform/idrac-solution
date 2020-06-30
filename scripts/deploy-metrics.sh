#! /bin/bash
set -e -m 

helm upgrade --install  --wait idracdemo-metrics charts/idracdemo-metrics \
--set image.repository=${DOCKER_REPOSITORY} \
--set image.tag=${IMAGE_TAG} \
--set fqn=<FQDN>

