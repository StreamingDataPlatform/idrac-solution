# flink SDP -> TimescaleDB

## Introduction

Visualize Processed iDRAC metrics using timescaleDB and Grafana.
This application will deploy the following components.
- TimescaleDB server running in SDP cluster
- Grafana server + iDRAC metrics dashboard

## Grafana URL

In this release, Grfana dashboard url won't be visible in the kubeapps application
the URL can be detected using the following:

Assuming the following:
- The external host for SDP is  externalHost: `sdp.sdp-demo.org`.
- SDP analytic project created is Project name: `idracsolution`.
- Assuming we created this release with Release name: `metrics`.

The Grafana URL for this combination : https://{{Release name}}-grafana.{{Project Name}}.{{externalHost}}.

Results: `https://metrics-grafana.idracsolution.sdp.sdp-demo.org/`.

## Values file sample

```
appParameters:
  # Events are written in a transaction and committed with this period.
  checkpointIntervalMs: "10000"

  # Set to false to allowing viewing metrics in Flink UI (may reduce performance).
  enableOperatorChaining: "false"
  # Read events from this stream.
  input-stream: "idracflatdata"
  input-startAtTail: "true"
  input-endAtTail: "false"
  input-minNumSegments: 1

jobName: IdracMetricsToTimescaleDB

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
timescaledb:
  externalHost: sdp.sdp-demo.org
  imageRegistry: sdp-registry:31001/desdp
  hooks:
    image:
      repository: nautilus-kubectl
  # timescalDB HA image
  image:
    repository: timescaledb-ha
  persistentVolumes:
    data:
      enabled: true
      size: 200Gi
      # sdp-local-path
      storageClass: "nfs"
    wal:
      enabled: true
      size: 5Gi
      # sdp-local-path
      storageClass: "nfs"
  database: tsdata
  patroni:
    postgresql:
      authentication:
        superuser:
          username: postgres
          password: password
```

## TimescaleDB Parameters

| Name                      | Description                                     | Value |
| ------------------------- | ----------------------------------------------- | ----- |
| `timescaledb.externalHost`    | externalHost                   | `"sdp.sdp-demo.org"`  |
| `timescaledb.database` | database | `"tsdata"`  |
| `timescaledb.patroni.postgresql.authentication.superuser.username`     | postgres username    | `"postgres"`  |
| `timescaledb.patroni.postgresql.authentication.superuser.username`    | postgres password               | `"password"`  |



## Flink Application Parameters

| Name                      | Description                                     | Value |
| ------------------------- | ----------------------------------------------- | ----- |
| `appParameters.input-stream`    | input stream                    | `"stream1"`  |
| `appParameters.checkpointIntervalMs` | Flink check points interval | `"1000"`  |
| `appParameters.enableOperatorChaining`     | enable operator chaining    | `"false"`  |
| `appParameters.input-startAtTail`    | start at tail of the stream                | `"true"`  |
| `appParameters.input-endAtTail` | End reading at the end of the stream | `"false"`  |
| `appParameters.input-minNumSegments`     | input stream minimum segments    | `"1"`  |
| `appParameters.output-minNumSegments`    | output stream minimum segments                    | `"1"`  |
