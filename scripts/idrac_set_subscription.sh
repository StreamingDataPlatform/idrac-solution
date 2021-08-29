#!/usr/bin/env bash

usage() {
	echo "Usage: $0 [ -u <idrac_user> -p <idrac_password> -h <idrac_host> -g <pravega_gateway> -r <rack_label> ]";
	exit 1;
}

while getopts u:p:h:g:r:: option
do
	case "${option}" in
		u) idrac_user=${OPTARG};;
		p) idrac_pass=${OPTARG};;
		h) idrac_host=${OPTARG};;
		g) gateway=${OPTARG};;
    r) racklabel=${OPTARG};;
	esac
done

if [ -z ${idrac_user} ] || [ -z ${idrac_pass} ] || [ -z ${idrac_host} ] || [ -z ${gateway} ] || [ -z ${racklabel} ] ;
then
	usage
fi

curl -o /tmp/idrac_resp -k -w "%{http_code}\n" \
-u ${idrac_user}:${idrac_pass} \
--header "Content-Type: application/json" \
--request POST \
--data "
{
\"Context\": \"Public\",
\"Protocol\": \"Redfish\",
\"EventFormatType\": \"MetricReport\",
\"EventTypes\": [\"MetricReport\"],
\"Destination\": \"https://${gateway}/v1/scope/idracsolution/stream/idracdata/event?routingKeyType=none&addField.RemoteAddr=${idrac_host}&addTimestamp=Timestamp&addField.RackLabel=${racklabel}\"
}
" \
https://${idrac_host}/redfish/v1/EventService/Subscriptions
