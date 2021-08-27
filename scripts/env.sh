# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

ROOT_DIR=$(readlink -f $(dirname $0)/..)
# Load environment variables from env-local.sh if it exists.
ENV_LOCAL_SCRIPT=$(dirname $0)/env-local.sh
if [[ -f ${ENV_LOCAL_SCRIPT} ]]; then
    source ${ENV_LOCAL_SCRIPT}
fi
NAMESPACE=idracsolution
APP_NAME=idracsolution
APP_GROUP_ID=${APP_GROUP_ID:-io.pravega.idracsolution}
APP_ARTIFACT_ID=${APP_ARTIFACT_ID:-flinkprocessor}
# Change line below to bump the application version.
APP_VERSION=${APP_VERSION:-0.0.4}
GRADLE_OPTIONS="${GRADLE_OPTIONS:-"-Pversion=${APP_VERSION}"}"
SDP_INSTALL_PATH={HOME}$/desdp
SDP_INSTALL_EXECUTABLE=/desdp/decks-installer/decks-install-linux-amd64
CERTS_PATH=${SDP_INSTALL_PATH}/certs
if [[ -f ${SDP_INSTALL_EXECUTABLE} ]]; then
    DOCKER_REGISTRY=$(${SDP_INSTALL_EXECUTABLE} config list | grep registry |  awk '{ print $2 }')
fi
