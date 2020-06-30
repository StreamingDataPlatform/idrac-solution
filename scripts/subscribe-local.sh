#!/usr/bin/env bash
curl -v \
-u Administrator:password \
--header "Content-Type: application/json" \
--request POST \
--data '
{
"Context": "Public",
"Protocol": "Redfish",
"EventFormatType": "MetricReport",
"EventTypes": ["MetricReport"],
"Destination": "http://10.246.21.231:3000/data"
}
' \
http://localhost:8443/redfish/v1/EventService/Subscriptions
