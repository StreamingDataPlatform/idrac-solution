#! /bin/bash
set -e -m 

printc() {
    B='\033[0;33m'
    NC='\033[0m'
    printf "${B} ${1} ${NC}\n"
}

printc "=================================================="
printc "==    Dell EMC Stream Analytics - Demo          =="
printc "==                Deploy Script                 =="
printc "=================================================="

ROOT_DIR="$(cd "$(dirname "$0")/.."; pwd -P)"

if [[ ! $DOCKER_REPOSITORY ]]
then
  printc "--- Docker repository not supplied! Exiting...  ---" 
  printUsage
  exit 1
else
  printc "--- Docker repository supplied!                 ---"
fi
  
echo
echo
printc "--- Provision Analytics project...  ---"

printc "---     Create namespace...         ---"
kubectl apply -f $ROOT_DIR/scripts/namespace.yaml

printc "---     Create Analytics Project... ---"
kubectl apply -f $ROOT_DIR/scripts/project.yaml


#echo
#echo
#printc "--- Moving credentials jar...                  ---"
#printc "        Source: $PRAVEGA_CREDENTIALS_JAR"
#printc "        Destination: $ROOT_DIR/gateway/lib/pravega-keycloak-credentials-shadow.jar"
#cp -L $PRAVEGA_CREDENTIALS_JAR $ROOT_DIR/gateway/lib/pravega-keycloak-credentials-shadow.jar

printc "---     Building Components....                ---"
$ROOT_DIR/scripts/build-k8s-components.sh $DOCKER_REPOSITORY

printc "--- Components built successfully!             ---"


function checkProjectMavenRepo() {
    READINESS="false"
    
    POD=`kubectl get pods --namespace idracsolution | grep repo | awk '{print $1}'`
    printc "---    Maven Repo Pod: $POD                    ---"

    while [ "$READINESS" != "true" ]; do
      READINESS=`kubectl get pod $POD --namespace idracsolution --output jsonpath="{.status.containerStatuses[0].ready}"`
      
      if [ "$READINESS" != "true" ]; then
        printc "       Waiting for Analytics Project Maven Repo pod readiness..." 
        sleep 5
      fi
      
    done
    sleep 5
    printc "--- Analytics Project maven Repo is Ready!     ---"
}

echo
echo
printc "--- Publish idracsolution Jar...                    ---"
checkProjectMavenRepo

printc "---     Starting port-forward to Maven repo... ---"
kubectl port-forward service/repo 9090:80 --namespace idracsolution &
sleep 5

printc "---     Building Flink Artifact...              ---"
$ROOT_DIR/gradlew :flinkprocessor:publish

printc "---     Stopping port-forward to Maven repo...  ---"
kill %1

printc "--- Flink Artifact deployed successfully!       ---"

echo
echo
printc "--- Deploying Components...                    ---"
$ROOT_DIR/scripts/deploy-k8s-components.sh $DOCKER_REPOSITORY
printc "--- Components deployed successfully!          ---"

exit

function checkElasticStack() {
    printc "--- Checking Elastic-Stack Deployment...       ---"
    READINESS="false"
    while [ "$READINESS" != "true" ]; do
      
      PODS=`kubectl get pods --namespace default | grep elastic-stack | awk '{print $1}'`
      READINESS="true"

      for POD in $PODS
      do 
        RUNNING=`kubectl get pod $POD --namespace default --output jsonpath="{.status.containerStatuses[0].ready}"`
        READINESS=$READINESS && $RUNNING
      done

      if [ "$READINESS" != "true" ]; then
        printc "       Waiting for Elastic-Stack pod readiness..."
        sleep 5
      fi
      
      sleep 5
      printc "--- Elastic-Stack Deployed Successfully!       ---"
    done
}

echo
echo
printc "--- Deploying Custom Kibana Dashboards...      ---"
checkElasticStack

printc "--- Starting port-forward to Socks5 proxy...   ---"
kubectl port-forward service/socks5-proxy 1080 --namespace kube-system &
sleep 5

printc "--- Deploy Kibana Dashboards...                ---"
$ROOT_DIR/scripts/import-kibana-dashboards.sh

printc "--- Stopping port-forward to Socks5 proxy...   ---"
kill %1

printc "--- Kibana dashboards deployed successfully!   ---"

echo 
echo
printc "=================================================="
printc "==    Dell EMC Stream Analytics - Demo          =="
printc "==             Deployment Complete              =="
printc "=================================================="
echo
echo
