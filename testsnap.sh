#!/bin/bash -xe

#####################################################################################
#   All the hard-coded files like requirements.yaml, values.yaml and retry          #
#   are added as part of the gerrit review https://gerrit.ericsson.se/#/c/6192237/  #
#####################################################################################

PUBLISHED_VERSION=${1//-SNAPSHOT/}
CHART_REPO=$2
PSWSEKA=$3
PSWSEKI=$4

NAMESPACE=testedc
RELEASE_NAME=eson-int
CHART_FOLDER=/home/helmuser/esoneps
USR=ejenksonom


helm repo add eson https://armdocker.rnd.ericsson.se/artifactory/proj-helm_aia-generic-local/releases/esoneps --username $USR --password $PSWSEKA
cd /home/helmuser
helm fetch eson/esoneps -d /home/helmuser/
tar -zxf esoneps-*.tgz
cp requirements.yaml values.yaml $CHART_FOLDER
cp cm-mediator-secret.yaml $CHART_FOLDER/templates

#wget https://raw.githubusercontent.com/kadwanev/retry/master/retry -O retry
chmod +x retry

sed -i -e 's/https:\/\/armdocker.rnd.ericsson.se\/artifactory\/proj-helm_aia-generic-local\/releases\/eric-event-data-collector/https:\/\/arm.seli.gic.ericsson.se\/artifactory\/proj-ec-son-ci-internal-helm/g' $CHART_FOLDER/requirements.yaml
sed -i '/eric-event-data-collector/,/version:.*/s/version:.*/version: "'$PUBLISHED_VERSION'"/' $CHART_FOLDER/requirements.yaml

helm repo add eric-data-coordinator-zk https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/ --username $USR --password $PSWSEKI
helm repo add eric-data-message-bus-kf https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/ --username $USR --password $PSWSEKI
helm repo add eric-data-document-database-pg https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-released-helm --username $USR --password $PSWSEKA
helm repo add eric-oss-schema-registry-sr https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm/ --username $USR --password $PSWSEKA
helm repo add eric-data-engine-sk https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm --username $USR --password $PSWSEKA
helm repo add eric-pm-server https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-pm-server-helm --username $USR --password $PSWSEKI
helm repo add eric-cm-mediator https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-cm-mediator-helm --username $USR --password $PSWSEKI
helm repo add eric-event-data-collector $CHART_REPO --username $USR --password $PSWSEKA
helm repo update

rm -rf $CHART_FOLDER/charts
cd $CHART_FOLDER
helm dependency update
cd /home/helmuser/
rm $CHART_FOLDER/requirements.lock

echo '#!/bin/bash' > createTopics.sh
echo 'kafka-topics --bootstrap-server eric-data-message-bus-kf:9092 --if-not-exists --create --replication-factor 3 --partitions 3 --topic epsFileInput' >> createTopics.sh
echo 'kafka-topics --bootstrap-server eric-data-message-bus-kf:9092 --if-not-exists --create --replication-factor 3 --partitions 3 --topic epsOut' >> createTopics.sh
echo 'kafka-topics --bootstrap-server eric-data-message-bus-kf:9092 --if-not-exists --create --replication-factor 3 --partitions 3 --topic filterEventList' >> createTopics.sh
echo 'kafka-topics --bootstrap-server eric-data-message-bus-kf:9092 --list' >> createTopics.sh

# Install
echo "Deploying eson integration chart"

helm install $RELEASE_NAME esoneps --namespace $NAMESPACE --values civalues.yaml

while true; do
      echo "checking the status of kafka pods"
      KAFKA_READY=`kubectl -n $NAMESPACE get pods | grep eric-data-message-bus-kf | awk '{ print $2 }' | grep -v 2/2 | wc -l`
      if [ 0 -eq ${KAFKA_READY} ]; then
          echo "kafka is running"
          kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- mkdir /tmp/files
          sleep 30
	  kubectl cp createTopics.sh $NAMESPACE/eric-data-message-bus-kf-0:/tmp/files
          kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- chmod +x /tmp/files/createTopics.sh
          kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- ./tmp/files/createTopics.sh
          break
      else
          sleep 30
      fi
done
while true; do
    echo "checking the status of the deployment"
    PODS_READY=`kubectl -n $NAMESPACE get pods | awk '{ print $3 }' | awk 'NR>1' | sort -u | grep -v Running | grep -v Completed | wc -l`
    if [ 0 -eq $PODS_READY ]; then
          break
    else
	      sleep 15
    fi
done

sleep 300

# Populate Schema-Registry
kubectl run -it schema-importer --restart=Never -n=testedc --image=armdocker.rnd.ericsson.se/proj-eson/celltrace-event-model:latest -- -dir=/pmdata/avro_schemas -registry=http://eric-oss-schema-registry-sr:8081

sleep 60

# CM mediation event filter population
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- curl -i -X POST -H "Content-Type: application/json" -d '{"name":"eventlist","title":"support event list schema","jsonSchema":{"$schema": "http://json-schema.org/draft-04/schema#", "type": "object", "properties": {"events": {"type": "array", "items": [{"type": "integer"}]}},"required": ["events"]}}' http://eric-cm-mediator:5003/cm/api/v1/schemas
sleep 10
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- curl -i -X POST -H "Content-Type: application/json" -d '{ "id": "eventlistsub", "configName": "eventlist", "event": ["configUpdated"], "callback": "kafka:filterEventList", "updateNotificationFormat": "full"}' http://eric-cm-mediator:5003/cm/api/v1/subscriptions
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- curl -i  http://eric-cm-mediator:5003/cm/api/v1/subscriptions/eventlistsub
etagNumber=`kubectl -n $NAMESPACE exec eric-data-message-bus-kf-0 -- curl -I http://eric-cm-mediator:5003/cm/api/v1/configurations/eventlist | grep ETag | awk -F':' '{print $2}'`
echo "Using etag $etagNumber"
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- curl -i -X POST -H "Content-Type: application/json" -d ' { "name": "eventlist", "title": "parser supported events", "data": { "events": [5153]}}' http://eric-cm-mediator:5003/cm/api/v1/configurations
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- curl -i -X PUT -H "Content-Type: application/json" -d "{\"title\": \"parser supported events\", \"baseETag\": \"${etagNumber}\", \"data\": {\"events\": [5153, 3108]}}" http://eric-cm-mediator:5003/cm/api/v1/configurations/eventlist
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- curl -i http://eric-cm-mediator:5003/cm/api/v1/configurations/eventlist

# Send data to input Kafka Topic
echo '#!/bin/bash' > produce.sh
echo 'kafka-console-producer --broker-list eric-data-message-bus-kf:9092 --topic epsFileInput < /tmp/files/15K1rop.json' >> produce.sh


sleep 30
kubectl cp 15K1rop.json $NAMESPACE/eric-data-message-bus-kf-0:/tmp/files

sleep 30
kubectl cp produce.sh $NAMESPACE/eric-data-message-bus-kf-0:/tmp/files
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- chmod +x /tmp/files/produce.sh
kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- ./tmp/files/produce.sh
sleep 300

# Validate output
eventsReceivedAll=`kubectl exec -n $NAMESPACE eric-data-message-bus-kf-0 -- curl http://eric-pm-server:9090/api/v1/query?query=sum%28kafka_log_Log_Value%7Btopic%3D%22epsOut%22%2C+name%3D%22LogEndOffset%22%7D%29 | sed 's/^.*,"/,"/' | sed 's/.*(\(.*\))/\1/' | grep -o '".*"' | tr -d '"'`
echo "Number of received events = $eventsReceivedAll"

helm uninstall $RELEASE_NAME -n $NAMESPACE
kubectl delete ns $NAMESPACE

if [ ${eventsReceivedAll} == "999483" ]; then
		echo "events received correct";
else
		echo "events received incorrect";
		exit 1
fi