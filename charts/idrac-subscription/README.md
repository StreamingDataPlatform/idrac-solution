# iDRAC Telemtric Subscription

# Introduction

This application allows the user to setup or delete telemetric subscription on iDRAC..
This subscription will push iDRAC metrics to SDP.

For more information about iDRAC telemetry service:

https://www.dmtf.org/sites/default/files/standards/documents/DSP0268_2021.2.pdf

## Parameters

value file example:
```
imageRegistry: sdp-registry:31001/desdp
image:
  repository: utility
  tag: 1.2.1
# Available method setup,delete
method: setup
idracConfig:
  # IDRAC_IP        IDRAC_USER   IDRAC_PASS       CALLBACK_URL                 RackLAbel
  - "10.25.20.125    root         calvin           gateway.example.com          examples"
```

| Name                      | Description                                     | Value |
| ------------------------- | ----------------------------------------------- | ----- |
| `method`    | setup or delete subscriptions from iDRAC servers              | `"setup"`  |
| `idracConfig` | array contain iDRAC IPs, authentication, SDP ingest gateway IP, RackLabel | `""`  |
