#! /bin/bash
# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
set -e
ROOT_DIR=$(readlink -f $(dirname $0)/..)
source ${ROOT_DIR}/scripts/env.sh
: ${NAMESPACE?"You must export NAMESPACE"}
: ${DOCKER_REGISTRY?"You must export DOCKER_REGISTRY"}

# Create project
kubectl apply -f ${ROOT_DIR}/scripts/project.yaml
PROJECT_STATUS=$(kubectl get project ${NAMESPACE} -n ${NAMESPACE}  -o 'jsonpath={..status.status}')
echo $PROJECT_STATUS
while [[ ${PROJECT_STATUS} != "Ready" ]];
   do echo "waiting for project ${NAMESPACE}" && sleep 5;
   PROJECT_STATUS=$(kubectl get project ${NAMESPACE} -n ${NAMESPACE}  -o 'jsonpath={..status.status}');
done
# Publish Application
${ROOT_DIR}/scripts/publish.sh

# Start a job
VALUES_FILE=${ROOT_DIR}/values/reportstoflatmetricvalues.yaml
export RELEASE_NAME=analytics
echo ${RELEASE_NAME}
helm upgrade --install --timeout 600s  --wait \
    ${RELEASE_NAME} \
    ${ROOT_DIR}/charts/analytics \
    --namespace ${NAMESPACE} \
    -f "${VALUES_FILE}" \
    --set "mavenCoordinate.artifact=${APP_ARTIFACT_ID}" \
    --set "mavenCoordinate.group=${APP_GROUP_ID}" \
    --set "mavenCoordinate.version=${APP_VERSION}" \
    --set "ingest-gateway.gateway.image=${DOCKER_REGISTRY}/ingest-gateway:1.3-3-3fdaba2" \
    --set "ingest-gateway.haproxy.image=${DOCKER_REGISTRY}/haproxy:2.2.14" \
    $@


# Create a psearch cluster
kubectl apply -f ${ROOT_DIR}/scripts/psearchcluster.yaml -n ${NAMESPACE}
sleep 5
PSEARCH_STATUS=$(kubectl get PravegaSearch psearch -n ${NAMESPACE}  -o 'jsonpath={..status.state}')
echo $PSEARCH_STATUS
while [[ ${PSEARCH_STATUS} != "Running" ]];
   do echo "waiting for psearch to be ready on ${NAMESPACE}" && sleep 5;
   PSEARCH_STATUS=$(kubectl get PravegaSearch psearch -n ${NAMESPACE}  -o 'jsonpath={..status.state}');
done

# add host to /etc/hosts
SEARCH_ING=$(kubectl get ing -n ${NAMESPACE?"You must export NAMESPACE"} psearch-resthead-headless -o jsonpath='{.spec.rules[0].host}')
ING_IP=$(kubectl get svc   nginx-ingress-controller -n nautilus-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
sudo sed -i "5i$ING_IP $SEARCH_ING" /etc/hosts
# locate psearch credentials
REST_ENCODED_AUTH=$(kubectl get  deployment psearch-ui -n ${NAMESPACE} -o json | jq -c '.spec.template.spec.containers[0].env[] | select(.name | contains("REST_ENCODED_AUTH"))'.value | tr -d '"')

# Create psearch stream index
if [[ $(kubectl get ing -n ${NAMESPACE} psearch-resthead-headless -o jsonpath="{.spec.tls}") == "" ]]; then
    export HTTP_PROTOCOL=http
else
    export HTTP_PROTOCOL=https
fi
REST_ENCODED_URL=${HTTP_PROTOCOL}://$(kubectl get ing -n ${NAMESPACE} psearch-resthead-headless -o jsonpath='{.spec.rules[0].host}')
echo $REST_ENCODED_URL
echo $REST_ENCODED_AUTH
curl -k -X POST -H "Content-Type: application/json" -H "Authorization: Basic $REST_ENCODED_AUTH" -d@${ROOT_DIR}/scripts/searcheable_stream.json ${REST_ENCODED_URL}/_searchable_streams

# Deploy grafana dashboard
sed "s/<base64generatedbasiccreds>/${REST_ENCODED_AUTH}/" ${ROOT_DIR}/dashboards/grafanaDashboard.yaml | kubectl apply  -n ${NAMESPACE} -f -
