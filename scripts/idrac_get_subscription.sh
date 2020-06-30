#!/usr/bin/env bash

usage() {
	echo "Usage: $0 [ -u <idrac_user> -p <idrac_password> -h <idrac_host> ]";
	exit 1;
}

while getopts u:p:h:: option
do
	case "${option}" in
		u) idrac_user=${OPTARG};;
		p) idrac_pass=${OPTARG};;
		h) idrac_host=${OPTARG};;
	esac
done

if [ -z ${idrac_user} ] || [ -z ${idrac_pass} ] || [ -z ${idrac_host} ];
then
	usage
fi

GET_SUBS="curl -s -k \
-u ${idrac_user}:${idrac_pass} \
--header \"Content-Type: application/json\" \
--request GET \
https://${idrac_host}/redfish/v1/EventService/Subscriptions"

status=$(eval ${GET_SUBS} | jq '.Members')

echo $status
