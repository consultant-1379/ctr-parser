#!/bin/bash

HELM_TEST_LOCAL=$(pwd)
PROJECT_ROOT="${HELM_TEST_LOCAL}/.."
SIGNUM=$( echo "$(whoami)" | tr '[:upper:]' '[:lower:]' | awk '{print substr($0,length($0)-6,7)}')
TAG=$(date +%Y%m%d-%H%M%S)

SERVICE_NAME="eric-oss-5gpmevent-filetrans-proc"
SERVICE_INTEGRATION_REPO_PATH="proj-ec-son-dev"
SERVICE_INTEGRATION_NAME="${SERVICE_NAME}-integration"
NAMESPACE="$SIGNUM-$SERVICE_INTEGRATION_NAME"

SERVICE_TAG="5fp-${TAG}-${SIGNUM}"
SERVICE_INTEGRATION_TAG="5fp-int-${TAG}-${SIGNUM}"
SERVICE_INTEGRATION_CHART_NAME="${SERVICE_NAME}-integration"
SERVICE_INTEGRATION_DEPLOYMENT_NAME="${SIGNUM}-${SERVICE_NAME}-integration"

# Service Requirements
SERVICE_REPO_PATH="proj-ec-son-dev"
#SERVICE_TAG="${TAG}"
SERVICE_VALUES=" --set images.eric-oss-5gpmevent-filetrans-proc.tag=${SERVICE_TAG},\
images.eric-oss-5gpmevent-filetrans-proc.name=${SERVICE_NAME},\
imageCredentials.pullSecret=${SERVICE_NAME}-secret,\
global.pullSecret=${SERVICE_NAME}-secret,\
imageCredentials.eric-oss-5gpmevent-filetrans-proc.repoPath=proj-ec-son-dev,\
imageCredentials.eric-oss-5gpmevent-filetrans-procTest.repoPath=proj-ec-son-dev,\
imageCredentials.eric-oss-5gpmevent-filetrans-procTest.tag=${SERVICE_INTEGRATION_TAG},\
imageCredentials.eric-oss-5gpmevent-filetrans-procTest.name=${SERVICE_INTEGRATION_CHART_NAME},\
celltrace.persistentVolumeClaim.enabled=true,\
celltrace.persistentVolumeClaim.claimName=${SERVICE_NAME}-pvc,\
celltrace.persistentVolumeClaim.mountPath='/tmp/files' "

SERVICE_VALUES_FILE=" --values ${PROJECT_ROOT}/charts/${SERVICE_NAME}/values.yaml "

# Service UG Requirements
SERVICE_UG_VALUES=" ${SERVICE_VALUES} "
SERVICE_UG_VALUES_FILE=" ${SERVICE_VALUES_FILE} -f ${PROJECT_ROOT}/charts/eric-oss-5gpmevent-filetrans-proc-integration/values.yaml "

# Integration Requirements
SERVICE_INTEGRATION_REPO_PATH="proj-ec-son-dev"
SERVICE_INTEGRATION_NAME="${SERVICE_NAME}-integration"
SERVICE_INTEGRATION_VALUES=" --set testsuite.image.repository=armdocker.rnd.ericsson.se/proj-ec-son-dev,\
testsuite.image.tag=${SERVICE_INTEGRATION_TAG},\
testsuite.image.name=${SERVICE_INTEGRATION_CHART_NAME},\
testsuite.image.deployment=${SERVICE_INTEGRATION_DEPLOYMENT_NAME},\
imageCredentials.pullSecret=${SERVICE_NAME}-secret,\
global.pullSecret=${SERVICE_NAME}-secret,\
imageCredentials.repository=armdocker.rnd.ericsson.se/${SERVICE_INTEGRATION_REPO_PATH} "

SERVICE_INTEGRATION_VALUES_FILE=" --values ${PROJECT_ROOT}/charts/eric-oss-5gpmevent-filetrans-proc/values.yaml -f ${PROJECT_ROOT}/charts/${SERVICE_INTEGRATION_NAME}/values.yaml "

# WA needed for NFS-Server to come up
SERVICE_RELEASE_NAME="eric-oss-5gpmevent-filetrans-proc"
SERVICE_INTEGRATION_RELEASE_NAME="eric-oss-5gpmevent-filetrans-proc-integration"

# Repositories
POSTGRES_SERVICE_NAME="eric-data-document-database-pg"
POSTGRES_SERVICE_CHART_REPO_URL="https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/"

SPARK_SERVICE_NAME="eric-data-engine-sk"
SPARK_SERVICE_CHART_REPO_URL="https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm"

ZOOKEEPER_SERVICE_NAME="eeric-data-coordinator-zk"
ZOOKEEPER_SERVICE_CHART_REPO_URL="https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/"

KAFKA_SERVICE_NAME="eric-data-message-bus-kf"
KAFKA_SERVICE_CHART_REPO_URL="https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/"

SCHEMA_REGISTRY_SERVICE_NAME="eric-oss-schema-registry-sr"
SCHEMA_REGISTRY_SERVICE_CHART_REPO_URL="https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm/"

EVENT_DATA_COLLECTOR_SERVICE_NAME="eric-event-data-collector"
EVENT_DATA_COLLECTOR_SERVICE_CHART_REPO_URL="https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm"

DATA_ENGINE_SERVICE_NAME="eric-data-engine-sk"
DATA_ENGINE_SERVICE_CHART_REPO_URL="https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm"

NFS_PROVISIONER_SERVICE_NAME="eric-nfs-provisioner"
NFS_PROVISIONER_SERVICE_CHART_REPO_URL="https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm"

PROMETHEUS_SERVICE_NAME="eric-pm-push-gateway"
PROMETHEUS_SERVICE_CHART_REPO_URL="https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm"

NC='\033[0m' # No Color
BROWN='\033[0;33m'

TIMEOUT_SVC_INSTALL="900"
TIMEOUT_INT_INSTALL="1200"
TIMEOUT_UPGRADE="900"
TIMEOUT_DEPLOYMENT_READY="300"
TIMEOUT_HELM_TESTS="2100"

function log() {
  echo -e "\n${BROWN} --- ${1} --- ${NC}\n"
}

function checkExitCode() {
  if [ $? -ne 0 ]; then
    log "ERROR: $1 "
    exit 255
  fi
}

function addRepos() {
  log "Initializing repositories"
  helm repo add eric-data-coordinator-zk https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/
  helm repo add eric-data-message-bus-kf https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/
  helm repo add eric-data-document-database-pg https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-released-helm
  helm repo add eric-schema-registry-sr https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm/
  helm repo add eric-data-engine-sk https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm
  helm repo add eric-pm-server https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-pm-server-helm
  helm repo add eric-cm-mediator https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-cm-mediator-helm
}

function cloneDevUtilsRepo() {
  if [ -d "${PROJECT_ROOT}/../../son-dev-utils" ]; then
    userInput "${PROJECT_ROOT}/../../son-dev-utils directory exists. Do you want this script to delete it before cloning (Yes/No)? [y/n] ?"
    DELETE_SONDEVUTILS_DIR="$?"
    if [ "$DELETE_SONDEVUTILS_DIR" -eq 1 ]; then
      rm -rf "${PROJECT_ROOT}/../../son-dev-utils"
    fi
  fi
  cd "${PROJECT_ROOT}/../"
  git clone ssh://${SIGNUM}@gerrit.ericsson.se:29418/OSS/com.ericsson.oss.services.sonom/son-dev-utils
  checkExitCode "Failed to clone son-dev-utils repo!"
}

#########
# function to cleanup the completed jobs : upgrade service
#########
function cleanFinishedJobs() {
  log "Cleaning completed jobs"
  JOBS=$(kubectl get jobs -n ${NAMESPACE} | awk '{ print $1 }' | grep -v NAME)
  (
    for JOB in ${JOBS}; do
      kubectl delete job ${JOB} -n ${NAMESPACE}
    done
  )
}

function buildDockerImagesCustom() {
  log "Building Docker Images"
  cd $PROJECT_ROOT
  mvn clean install -Dskip-unit-tests=true -DskipTests -Dmaven.test.skip=true --settings ../aia_settings.xml
  checkExitCode "Building the service ${SERVICE_NAME} with maven failed"
  mvn clean install -f ../testsuite/pom.xml -U -V -B --settings ../aia_settings.xml
  checkExitCode "Building the integration test ${SERVICE_INTEGRATION_NAME} with maven failed"

  log "Building armdocker.rnd.ericsson.se/${SERVICE_REPO_PATH}/${SERVICE_NAME}:${SERVICE_TAG}"
  docker build . -t armdocker.rnd.ericsson.se/$SERVICE_REPO_PATH/$SERVICE_NAME:$SERVICE_TAG
  checkExitCode "Failed to build armdocker.rnd.ericsson.se/${SERVICE_REPO_PATH}/${SERVICE_NAME}:${SERVICE_TAG}"

  log "Building armdocker.rnd.ericsson.se/${SERVICE_INTEGRATION_REPO_PATH}/${SERVICE_INTEGRATION_NAME}:${SERVICE_INTEGRATION_TAG}"
  cd "../testsuite/integration/jee/"
  docker build . -t armdocker.rnd.ericsson.se/$SERVICE_INTEGRATION_REPO_PATH/$SERVICE_INTEGRATION_NAME:$SERVICE_INTEGRATION_TAG
  checkExitCode "Failed to build armdocker.rnd.ericsson.se/${SERVICE_INTEGRATION_REPO_PATH}/${SERVICE_INTEGRATION_NAME}:${SERVICE_INTEGRATION_TAG}"

  if [[ "$remote" -eq 1 ]]; then
    docker push armdocker.rnd.ericsson.se/${SERVICE_REPO_PATH}/${SERVICE_NAME}:${SERVICE_TAG}
    checkExitCode "Pushing docker image ${SERVICE_NAME} to repository failed"
    docker push armdocker.rnd.ericsson.se/${SERVICE_INTEGRATION_REPO_PATH}/${SERVICE_INTEGRATION_NAME}:${SERVICE_INTEGRATION_TAG}
    checkExitCode "Pushing docker image ${SERVICE_INTEGRATION_NAME} to repository failed"
  fi

  cd ${HELM_TEST_LOCAL}
}

function runGenericHelmTestScript() {
  cd "${HELM_TEST_LOCAL}"
  source ${PROJECT_ROOT}/../../son-dev-utils/scripts/genericHelmTestLocalScript.sh

  getUserOptions
  ensureOnCorrectServer
  addRepos
  initRepo
  cleanUpHelmEnvironment
  buildDockerImagesCustom

  cleanFinishedJobs
  installService
  installIntegrationService
  waitForDeploymentReady

  runBasicHelmTests
  runHelmTest
}

#########
# main
#########
inputArg=$(echo $1 | tr '[:upper:]' '[:lower:]')
if [[ "$inputArg" == *"clone"* ]]; then
  cloneDevUtilsRepo
fi

runGenericHelmTestScript

if [ "$cleanAfter" -eq 1 ]; then
  cleanUpHelmEnvironment
fi
