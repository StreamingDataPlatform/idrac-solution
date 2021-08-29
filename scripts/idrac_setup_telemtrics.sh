#!/bin/bash

ROOT_DIR=$(dirname $0)

while IFS= read -r LINE; do
        # Skip commented lines
	case "$LINE" in \#*) continue ;; esac
	ARRAY=(${LINE})
        # Data is in the following form
	# IDRAC_IP  USER  PASSWD CALLBACK_URL
	IP="$(echo -e  ${ARRAY[0]} | tr -d '[:space:]')"
	USER="$(echo -e  ${ARRAY[1]} | tr -d '[:space:]')"
	PASS="$(echo -e  ${ARRAY[2]} | tr -d '[:space:]')"
	GATEWAY="$(echo -e  ${ARRAY[3]} | tr -d '[:space:]')"
	RACKLABEL="$(echo -e  ${ARRAY[4]} | tr -d '[:space:]')"
	# Set subscription using redifish API
	echo "####################################################################"
  echo $IP $USER $PASS $GATEWAY $RACKLABEL
  echo "####################################################################"
	status=$(${ROOT_DIR}/idrac_set_subscription.sh -u ${USER} -p ${PASS} -h ${IP} -g ${GATEWAY}  -r ${RACKLABEL})
        # Enable telemetrics reports
	if [ $status != "201" ] ; then
           echo "{\"message\": [ $(cat /tmp/idrac_resp) ], \"status\": \"failed to set subscription\", \"http_status_code\": \"${status}\"}"
	   continue
	fi
	${ROOT_DIR}/idrac_enable_attributes.sh -u $USER -p $PASS -h $IP
done < ${1} 
