#! /bin/bash
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

ROOT_DIR=$(readlink -f $(dirname $0)/..)
source ${ROOT_DIR}/installer/env.sh
: ${NAMESPACE?"You must export NAMESPACE"}


# Create project
helm upgrade --install --timeout 600s --debug --wait \
    analytic-project \
    ${ROOT_DIR}/charts/project \
    --namespace ${NAMESPACE} \
    $@

# Publish Application
${ROOT_DIR}/installer/publish.sh

# Start a job
VALUES_FILE=${ROOT_DIR}/values/reportsToFlatMetricvalues.yaml
export RELEASE_NAME=$(basename "${VALUES_FILE}" .yaml)

helm upgrade --install --timeout 600s --debug --wait \
    ${RELEASE_NAME} \
    ${ROOT_DIR}/charts/analytics \
    --namespace ${NAMESPACE} \
    -f "${VALUES_FILE}" \
    --set "mavenCoordinate.artifact=${APP_ARTIFACT_ID}" \
    --set "mavenCoordinate.group=${APP_GROUP_ID}" \
    --set "mavenCoordinate.version=${APP_VERSION}" \
    $@

# Deploy ingest gateway
#TODO
# Create a psearch cluster
helm upgrade --install --timeout 600s --debug --wait \
    analytic-psearch \
    ${ROOT_DIR}/charts/psearch \
    --namespace ${NAMESPACE} \
    $@

# locate psearch credentials
REST_ENCODED_AUTH=$(kubectl get  deployment psearch-kibana -n ${NAMESPACE} -o json | jq -c '.spec.template.spec.containers[0].env[] | select(.name | contains("REST_ENCODED_AUTH"))'.value)

# Create psearch stream index
if [[ $(kubectl get ing -n ${NAMESPACE} psearch-resthead-headless -o jsonpath="{.spec.tls}") == "" ]]; then
    export HTTP_PROTOCOL=http
else
    export HTTP_PROTOCOL=https
fi
REST_ENCODED_URL=${HTTP_PROTOCOL}://$(kubectl get ing -n ${NAMESPACE} psearch-resthead-headless -o jsonpath='{.spec.rules[0].host}')}
curl -k -X POST -H "Content-Type: application/json" \
     -H "Authorization: Basic ${REST_ENCODED_AUTH}"\
     -d'{"streamName": "idracflatdata","indexConfig": {"name": "idracflatdata-index"}}' \
     ${REST_ENCODED_URL}/_searchable_streams

# Deploy grafana dashboard
sed "s/<base64generatedbasiccreds>/${REST_ENCODED_AUTH}/" ${ROOT_DIR}/dashboards/grafanaDashboard.yaml -n ${NAMESPACE}  | kubectl apply -f -