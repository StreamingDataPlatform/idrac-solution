#!/usr/bin/env bash

usage() {
	echo "Usage: $0 [ -u <idrac_user> -p <idrac_password> -h <idrac_host> -s <subscription_id>]";
	exit 1;
}

while getopts u:p:h:s:: option
do
	case "${option}" in
		u) idrac_user=${OPTARG};;
		p) idrac_pass=${OPTARG};;
		h) idrac_host=${OPTARG};;
		s) sub_id=${OPTARG};;
	esac
done

if [ -z ${idrac_user} ] || [ -z ${idrac_pass} ] || [ -z ${idrac_host} ] || [ -z ${sub_id} ];
then
	usage
fi

curl -v -k \
-u ${idrac_user}:${idrac_pass} \
--header "Content-Type: application/json" \
--request DELETE \
https://${idrac_host}/redfish/v1/EventService/Subscriptions/${sub_id}

