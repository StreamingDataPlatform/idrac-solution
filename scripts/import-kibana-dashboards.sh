#! /bin/bash
echo "Import Dashboard to kibana..."

SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )

ROOT_DIR=$SCRIPT_DIR/..

DASHBOARD_1=$ROOT_DIR/taxidemo-kibana-export-api-1.json
echo "Dashboard: $DASHBOARD_1"

DASHBOARD_2=$ROOT_DIR/taxidemo-kibana-export-api-2.json
echo "Dashboard: $DASHBOARD_2"

SERVICE_NAME=elastic-stack-kibana.default
CLUSTER=svc.cluster.local
PORT=443
API=api/kibana/dashboards/import?exclude=some_pattern
KIBANA_URL=http://$SERVICE_NAME.$CLUSTER:$PORT/$API
echo "Kibana URL: $KIBANA_URL"

for DASHBOARD in $DASHBOARD_1 $DASHBOARD_2
do 
    echo "Importing dashboard: $DASHBOARD..."
    curl \
        --location \
        --include \
        --insecure \
        --proxy socks5h://localhost:1080 \
        --request POST \
        --url $KIBANA_URL \
        --header 'kbn-xsrf: true' \
        --header 'Content-Type: application/json' \
        --data @$DASHBOARD
    echo 
done