#! /bin/bash
set -e -m 

helm upgrade --install  --wait idracsolution-metrics charts/idracsolution-metrics \
--set image.repository=${DOCKER_REPOSITORY} \
--set image.tag=${IMAGE_TAG} \
--set fqn=<FQDN>

