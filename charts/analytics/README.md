# iDRAC Solution

## Introduction

This is a solution developed to run in Streaming data Platform.
It collects data from iDRAC server using the telemetry feature. This data is then processed in SDP processing engine and finally pushed for visualization in Grafana. 

## iDRAC metrics

 iDRAC (integrated dell remote access controller) has been running on dell servers for quite some time.  iDRAC provides remote access to the server for power management, deployment and recently monitoring with telemetry feature. This feature when enabled, it will push metric reports (like Amps reading, Network traffic etc.) to a configurable destination.

## What's in the helm chart?
 This helm chart contains the following:

 - Flink clusters - Flink cluster resource for data processing
 - Flink applications - Flink application resource for Flink jobs
 - Secure Gateway - A Java HTTP server that simulates an IOT Gateway. Events are sent to this gateway and stored in a Pravega Stream.

## Values file sample
```
appParameters:
  # Events are written in a transaction and committed with this period.
  checkpointIntervalMs: "10000"

  # Set to false to allowing viewing metrics in Flink UI (may reduce performance).
  enableOperatorChaining: "false"

  # Read events from this stream.
  input-stream: "idracdata"
  input-startAtTail: "false"
  input-endAtTail: "false"
  input-minNumSegments: 1
  # Write events to this stream.
  output-stream: "idracflatdata"
  output-minNumSegments: 1

# Job Name
jobName: MetricReportToSingleMetricValue

# Flink app coordinates
mavenCoordinate:
  artifact: flinkprocessor
  group: io.pravega.idracsolution
  version: 0.1.0

flinkVersion: 1.13.2
imageRef:
  name: flink-1.13.2

localStorage:
  replicas: 2
  size: "20G"

volumes: []

parallelism: 1

jobManager:
  cpu: "500m"
  memory: "2048M"
  replicas: 1

taskManager:
  numberOfTaskSlots: 2
  replicas: 1
  resources:
    requests:
      memory: "2Gi"
      cpu: "1000m"
    limits:
      memory: "2Gi"
      cpu: "1000m"

logging: {}
  #io.pravega: DEBUG
  #io.pravega.connectors: DEBUG
#org.apache.flink: DEBUG
```