# Dell EMC iDRAC Nautilus Demo

## What's in the helm chart?
This helm chart contains the following:

- Elastic stack - A Elastic search and kibana cluster for data visualization 

- analytic-projects - A Nautilus project

- analytic - containing
  - Flink clusters - Flink cluster resource for data processing
  - Flink applications - Flink application resource for Flink jobs
  - gateway - A Java HTTP server that simulates an IOT Gateway.  Events are sent to this gateway and stored in a Pravega Stream
  - webcrawler - python application used here to pull data from IDRAC Idrac FW team http server; Data in this http server are from 30+ different IDRAC servers and are updated every 1 minute.
  
## Quick Demo Deployment to Nautilus

### Build docker images

If your network requires non-standard TLS certificates to be trusted during the build process, 
place them in the `ca-certificates` directory.

```
#export DOCKER_REPOSITORY=<hostname>:<port>/<namespace>
export DOCKER_REPOSITORY=hop-claudio-minikube-1.solarch.lab.emc.com:31001
export IMAGE_TAG=0.0.1-localdev
scripts/build-k8s-components.sh
```
### Create the project
```
kubectl apply -f scripts/namespace.yaml 
kubectl apply -f scripts/project.yaml
```
### Upload artifact

After the project is ready, we should upload the artifact manually. 

First, we start a port forward session to the Project's Maven Repository. 

Then, we build and upload the Flink Job Jar.

To build we need Flink Connector for influx DB from https://github.com/apache/bahir-flink
Clone the repo and build the jar. 
Copy the jar to ~/flinkprocessor/libs
uncomment the compile tag in  ~/flinkprocessor/build.gradle
```
export MAVEN_USER=desdp
export MAVEN_PASSWORD=$(kubectl get secret keycloak-desdp -n nautilus-system -o jsonpath='{.data.password}' | base64 -d)
kubectl port-forward svc/repo 9091:80 -n idracdemo
./gradlew :flinkprocessor:publish
```
### Deploy Components

Deploy influxdb and grafana

Change the URL of cluster and deploy 

```
fqn=<hostname>
scripts/deploy-metrics.sh
```
Finally deploy the demo components to K8s
```
scripts/deploy-k8s-components.sh
```
### Data Simulator

To deploy the webcrawler 
```
webcrawler/deploy-k8s-webcrawler.sh
```
To deploy synthetic data generator
```
synthetic_data_generator/deploy-k8s-synthetic.sh
```
### Configure iDRAC live data

For live data from IDrac servers, you can use the following script; where `scripts/idrac-sample.config`
contains the information of the IDRAC server(follow the sample in `scripts/idrac-sample.config`)
```
./scripts/idrac_setup_telemtrics.sh scripts/idrac-sample.config
```
The idrac demo provides two diffent implementations to visualise the dashboards

### Visualisation configurations

Grafana Dashboard UI URL

use admin/passwod to access grafana dashboard
```
grafana-idracdemo.<hostname>
```

Kibana Dashboard:
```
kubectl port-forward svc/idracdemo-elastic-stack-kibana 9092:443 -n idracdemo &
```
Kibana will be available at http://localhost:9092/

### Expose external connectivity

If we want to enable external connectivity for the Elastic client and Kibana. We can change the 
`<hostname>` with your own host name and run below command.
```
kubectl apply -f scripts/es-ingress.yaml
```

Enable connectivity to gateway for IDRAC servers
```
kubectl apply -f scripts/gateway-ingress.yaml
```


After the ingress resource is ready, we can get ES client at <http://es-client.idracdemo.<hostname>>,  Kibana at <http://kibana.idracdemo.<hostname>>

## Building and Running the Demo

In the steps below, sections noted with **(Nautilus SDK Desktop)** should only be performed
in a Nautilus SDK Desktop in a Kubernetes deployment of Nautilus.
Sections noted with **(Local)** should only be performed in a local workstation deployment
of Pravega.

### Download this Repository

```
cd
git clone --recursive https://github.com/pravega/idrac-demo
cd idrac-demo
```

### (Local) Install Operating System

Install Ubuntu 16.04 LTS. Other operating systems can also be used but the commands below have only been tested
on this version.

### (Local) Install Java 8

```
apt-get install openjdk-8-jdk
```

### (Local, Optional) Install IntelliJ

Install from <https://www.jetbrains.com/idea>.
Enable the Lombok plugin.
Enable Annotations (settings -> build, execution, deployment, -> compiler -> annotation processors).

### (Local) Install Docker and Docker Compose

See <https://docs.docker.com/install/linux/docker-ce/ubuntu/>
and <https://docs.docker.com/compose/install/>.

### (Local) Run Pravega

This will run a development instance of Pravega locally.
Note that the default *standalone* Pravega used for development is likely insufficient for testing because
it stores all data in memory and quickly runs out of memory.
Using the procedure below, all data will be stored in a small HDFS cluster in Docker.

In the command below, replace x.x.x.x with the IP address of a local network interface such as eth0.

```
cd
git clone https://github.com/pravega/pravega
cd pravega
git checkout r0.6
cd docker/compose
export HOST_IP=x.x.x.x
docker-compose up -d
```

You can view the Pravega logs with `docker-compose logs --follow`.

You can view the stream files stored on HDFS with `docker-compose exec hdfs hdfs dfs -ls -h -R /`.

### Run the Pravega Gateway

From your Desktop Set the Pravega controller.
Skip this command if you have a local Pravega installation.
```
export PRAVEGA_CONTROLLER=tcp://<nautilus pravega controller DN>:9090
```

Run the Pravega Gateway.
```
export PRAVEGA_SCOPE=idracdemo
export PRAVEGA_STREAM=idracdata
./gradlew gateway:run
```

### Run the Flink Job.

This will run a streaming Flink job.

Run the Flink app in `flinkprocessor` with the following parameters:
```
--jobClass
io.pravega.example.idracdemo.flinkprocessor.jobs.PravegaStringToConsoleJob
--controller
tcp://127.0.0.1:9090
--scope
idracdemo
--input-stream
idracdata
```

Run the Flink app in `flinkprocessor` with the following parameters:
```
--jobClass
io.pravega.example.idracdemo.flinkprocessor.jobs.PravegaMetricReportToElasticSearchJob
--controller
tcp://127.0.0.1:9090
--scope
idracdemo
--input-stream
idracdata
--elastic-host
hop-claudio-minikube-1.solarch.lab.emc.com
--elasticsearch-client
31718
--elastic-index
idracdemo-metrics
--elastic-type
FlatMetricReport
--elastic-delete-index
true
```
