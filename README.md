# Dell EMC iDRAC Streaming Data Platform Solution

### Create a Self-Signed certificate for HA-Proxy

- Create a Private Key and Certificate signing Request file
  ```
  openssl genrsa -out private.key
  openssl req -new -key private.key -out request.csr
  ```
- Create a .CRT File
  ```
  openssl x509 -req -days 365 -in request.csr -signkey private.key -out certificate.crt
  ```
- copy the content of the .crt file and the .key file to `idrac-demo/charts/analytics/charts/ingest-gateway/templates/haproxy-cert.yaml`

### Build deployment

```
./scripts/build_installer.sh
```


### deploy using SDP OVA
```
tar xzvf idracsolution-0.0.4.tgz
cd idracsolution
./scripts/install_on_sdp.sh
```

### Configure iDRAC live data

For live data from IDrac servers, you can use the following script; where `scripts/idrac-sample.config`
contains the information of the IDRAC server(follow the sample in `scripts/idrac-sample.config`)
```
./scripts/idrac_setup_telemtrics.sh scripts/idrac-sample.config
```


# Contributing
If you would like to contribute to this repository, please follow the [Contribution Guidelines](https://github.com/StreamingDataPlatform/idrac-demo/wiki/Contributing).
