#! /bin/bash
set -ex

NAMESPACE=idracsolution

helm uninstall \
${NAMESPACE}-elastic-stack \
--namespace ${NAMESPACE}

# helm del --purge \
# ${NAMESPACE}-synthetic-data-generator

helm uninstall \
${NAMESPACE}-analytic \
--namespace ${NAMESPACE}

helm  uninstall \
${NAMESPACE}-project \
--namespace ${NAMESPACE}
