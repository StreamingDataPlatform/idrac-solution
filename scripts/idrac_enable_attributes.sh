#!/bin/bash

usage() {
	echo "Usage: $0 [ -u <idrac_user> -p <idrac_password> -h <idrac_host> ]";
	exit 1;
}

while getopts u:p:h:: option
do
	case "${option}" in
		u) USER=${OPTARG};;
		p) PASS=${OPTARG};;
		h) IP=${OPTARG};;
	esac
done

if [ -z ${USER} ] || [ -z ${PASS} ] || [ -z ${IP} ];
then
	usage
fi

# Disable list

DISABLE_ATTRIBUTES=()

MY_JSON="/tmp/myjson.json"
jq -n  '{}' > ${MY_JSON}

for ATTRIBUTE in "${DISABLE_ATTRIBUTES[@]}"
do
   echo $(jq --arg key "Telemetry${ATTRIBUTE}.1.EnableTelemetry" --arg value "Disabled" '.Attributes[$key] = $value' ${MY_JSON}) > ${MY_JSON}
done


MY_ATTRIBUTES=( \
	"FanSensor" \
        "PowerMetrics" "ThermalSensor" "ThermalMetrics" "MemorySensor" \
	"CPUMemMetrics" \
	"PSUMetrics" "AggregationMetrics" \
	"NICStatistics" "NICSensor" "FCSensor" "FPGASensor" \
        "ThermalSensor" "PowerStatistics" "CUPS" "StorageDiskSMARTData" \
        "GPUStatistics" "NVMeSMARTData" "CPURegisters" "SerialLog" "GPUMetrics" \
        "Sensor" "StorageSensor" "MemorySensor" "CPUSensor")


for ATTRIBUTE in "${MY_ATTRIBUTES[@]}"
do
  echo $(jq --arg key "Telemetry${ATTRIBUTE}.1.EnableTelemetry" --arg value "Enabled" '.Attributes[$key] = $value' ${MY_JSON}) > ${MY_JSON}
done

curl -k -u ${USER}:${PASS} --header "Content-Type: application/json" --request PATCH --data "@${MY_JSON}" "https://${IP}/redfish/v1/Managers/iDRAC.Embedded.1/Attributes"
