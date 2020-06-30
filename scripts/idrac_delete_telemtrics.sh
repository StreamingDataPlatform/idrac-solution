#!/bin/bash

ROOT_DIR=$(dirname $0)


get_sub_detatils() {
	curl  -s -k \
	-u ${1}:${2} \
	--header "Content-Type: application/json" \
	--request GET \
	https://${3}${4}
}

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
	# Get subscriptions using redifish API
	status=$(${ROOT_DIR}/idrac_get_subscription.sh -u ${USER} -p ${PASS} -h ${IP})
	for row in $(echo "${status}" | jq -r '.[] | @base64'); do
	    _jq() {
	     echo ${row} | base64 --decode | jq -r '.[]'
	    }
	    id=$(_jq)
	    echo $id
	    GET_SUB_BY_ID=$(get_sub_detatils ${USER} ${PASS} ${IP} ${id})
	    data=$(echo $GET_SUB_BY_ID)
	    callback=$(echo $data | jq  -r '.Destination')
	    sub_id=$(echo $data | jq  -r '.Id')
	    if [[ ${callback} =~ ${GATEWAY} ]]
            then
		    echo ${callback} ${sub_id}
		    # scripts/idrac_delete_subscription.sh
		    ${ROOT_DIR}/idrac_delete_subscription.sh -u ${USER} -p ${PASS} -h ${IP} -s ${sub_id}
	    fi

	done
	# Search subscriptions that match callback
	# ${ROOT_DIR}/idrac_delete_subscription.sh -u $USER -p $PASS -h $IP_id
done < ${1} 
