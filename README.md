# Dell EMC iDRAC Streaming Data Platform Solution

  
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
