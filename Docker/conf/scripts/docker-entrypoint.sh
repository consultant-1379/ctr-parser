#!/bin/bash
# Startup script for the ESN APEPS LSB service instance
#
# chkconfig: 2345 85 15
# description: Init script for Avro Aligned Parser EPS
# pidfile: /var/run/<APEPS_INSTANCE_ID>.pid
#
# In this section, update required-start and required-stop for any lsb services required before
# this service starts or stops

#Retrieve the environment variables from Docker
# for ":-" If variable not set or null, substitute default.
# for ":=" If variable not set or null, assign default.
APP_ID="${APPLICATION_ID}"
APP_HOME="${APPLICATION_HOME}"
LOGBACK_FILE="${LOGBACK_FILE}"
EPS_FLOW_DIRECTORY="${FLOW_DIRECTORY}"
MAX_HEAP_SIZE_GB="${MAX_HEAP_SIZE_GB:-1500M}"
INITIAL_HEAP_SIZE_GB="${INITIAL_HEAP_SIZE_GB:-1500M}"
INSTRUMENTATION_METHOD="${INSTRUMENTATION_METHOD:-JMX}"
INSTRUMENTATION_LOCATION="${INSTRUMENTATION_FILE_LOCATION}"
REMOTE_JMX_PORT="${JMX_PORT:-21000}"
SCHEMA_REGISTRY="${SCHEMA_REGISTRY}"
INSTRUMENTATION_ON="${INSTRUMENTATION_ON}"
CMMEDIATOR_SUBSCRIBER_INTEGRATION_POINT_FILE="${CMMEDIATOR_SUBSCRIBER_FILE:=notDefined.ipl}"
EPS_IPL_DIRECTORY="${IPL_DIRECTORY:=/ericsson/apeps/flow}"
CM_IPL_FULL_PATH="${EPS_IPL_DIRECTORY}/${CMMEDIATOR_SUBSCRIBER_INTEGRATION_POINT_FILE}"
STAGING_DIR=${STAGING_MOUNT_PATH}
ROOT_LOGGER_LEVEL="${ROOT_LOGGER_LEVEL:-info}"

#application variables
EPS_INSTANCE_ID="${APPLICATION_ID}"
APEPS_LOG_DIR="${APPLICATION_HOME}/log/"
EPS_LIB_FILES="${APPLICATION_HOME}/lib/*"
ENV_FILE="${APPLICATION_HOME}/etc/${APP_ID}.env"
EPS_HOME="/ericsson/eps"
DEFAULT_CLASSPATH="$EPS_HOME/ext-lib/*:$EPS_HOME/lib/*:"
DEFAULT_MAIN_CLASS_NAME="com.ericsson.component.aia.services.eps.core.main.EpsApplication"
HOSTNAME=$(hostname)
DEFAULT_ZOOCONFIG_PATH="/etc/opt/ericsson/zookeeper/conf/"
DEFAULT_JAVA="/usr/bin/java"

HOST_ID=${HOSTNAME##*-}

if [ ! -z "${JAVA_HOME}" ]; then
    JAVA=${JAVA_HOME}/bin/java
else
    JAVA=$DEFAULT_JAVA
fi

AWK=/usr/bin/awk
WC=/usr/bin/wc
CAT=/bin/cat
ECHO=/bin/echo
HEAD=/usr/bin/head
LOGGER=/usr/bin/logger
LS=/bin/ls
MKDIR=/bin/mkdir
RM=/bin/rm
SCRIPT_NAME=$(basename $0)
SED=/bin/sed
TOUCH=/usr/bin/touch
EGREP=/bin/egrep
PGREP=/usr/bin/pgrep
GREP=/bin/grep
IFCONFIG=/sbin/ifconfig
CUT=/bin/cut
TR=/usr/bin/tr
ADDR="addr"
DEFAULT_CACHE_PORT=11322

host=${HOSTNAME}

STOP_TIMEOUT=${STOP_TIMEOUT-10}

#TODO: need to be replaced to detect the memory limit for the container
check_memory_on_kvm(){
    echo "Checking memory on kvm"
}

create_directory(){
    local directory=$1
    if [ ! -d $directory ] ; then
        echo "Creating directory $directory"
        $MKDIR -p $directory
        if [ $? -ne 0 ]
        then
            error "Cannot create directory $directory"
            exit 10
        fi
    fi
}

copyMountedFilesFromStagingToFlowAndIplLocations(){
    $ECHO "Copying files from staging"
    cp ${STAGING_DIR}/*.json ${EPS_IPL_DIRECTORY}/
    cp ${STAGING_DIR}/*.xml  ${EPS_FLOW_DIRECTORY}/
}

updateIPLGroupIDWithHostId(){

    if [ -e ${CM_IPL_FULL_PATH} ]; then

        INDEX_OF_GROUP_ID=0
        for entry in `cat ${CM_IPL_FULL_PATH} | jq '.properties' | jq -r ".[] | .name"`; do
            if [ "$entry" == "group.id" ]; then
                echo "Index of group.id found: ${INDEX_OF_GROUP_ID}"
                break;
            fi
            INDEX_OF_GROUP_ID=$(($INDEX_OF_GROUP_ID + 1))
        done

        GROUP_ID_VALUE=`cat ${CM_IPL_FULL_PATH} | jq -r ".properties[$INDEX_OF_GROUP_ID].value"`
        NEW_GROUP_ID_VALUE="${GROUP_ID_VALUE}-${HOST_ID}"
        $ECHO "Found the value $GROUP_ID_VALUE for group.id and will replace it with ${NEW_GROUP_ID_VALUE}"
        $SED -i "s/${GROUP_ID_VALUE}/${NEW_GROUP_ID_VALUE}/" ${CM_IPL_FULL_PATH}
        $ECHO "The expected cm mediator subscriber file has been updated with a host ID: ${HOST_ID}"
    else
        $ECHO "The expected cm mediator subscriber file was not found, the consumer group.id has not been updated for this consumer"
        $ECHO "Expected to find ${CM_IPL_FULL_PATH}"
    fi

}

set_eps_jvm(){

    jvm_prop+=" -DEPS_INSTANCE_ID=$EPS_INSTANCE_ID"
    jvm_prop+=" -Dlogback.configurationFile=${LOGBACK_FILE}"
    jvm_prop+=" -Dcom.ericsson.component.aia.itpf.sdk.external.configuration.folder.path=$DEFAULT_ZOOCONFIG_PATH"
    jvm_prop+=" -Dcom.ericsson.component.aia.services.eps.core.flow.deployment.failure.policy=STOP_JVM"
    jvm_prop+=" -server"
    jvm_prop+=" -XX:+AggressiveOpts"                   # Turn on point performance compiler optimizations that are expected to be default in upcoming releases.
    jvm_prop+=" -XX:+UseCompressedOops"                # Enables the use of compressed pointers for optimized 64-bit performance with Java heap sizes less than 32gb.
    jvm_prop+=" -XX:+TieredCompilation"
    jvm_prop+=" -XX:+UseBiasedLocking"
    jvm_prop+=" -Djava.net.preferIPv4Stack"
    jvm_prop+=" -XX:+DisableExplicitGC"
    jvm_prop+=" -Dsun.rmi.dgc.server.gcInterval=3600000 -Dsun.rmi.dgc.client.gcInterval=3600000 "
    jvm_prop+=" -Dcom.sun.management.jmxremote"
    jmx_prop+=" -Dcom.sun.management.jmxremote.port=${REMOTE_JMX_PORT}"
    jmx_prop+=" -Dcom.sun.management.jmxremote.ssl=false"
    jmx_prop+=" -Dcom.sun.management.jmxremote.authenticate=false "
    jmx_prop+=" -Dcom.sun.management.jmxremote.rmi.port=${REMOTE_JMX_PORT}"
    jmx_prop+=" -Djava.rmi.server.hostname=0.0.0.0"

}

set_jmx_tag(){
    local jmx_tag_from_hostname="$EPS_INSTANCE_ID"
    echo "JMX Tag for DDC is ${jmx_tag_from_hostname}"
    jvm_prop+="-Ds=${jmx_tag_from_hostname}"
    if [ "${INSTRUMENTATION_ON}" == "true" ];then
        jvm_prop+=" -Dcom.ericsson.component.aia.services.eps.core.statistics.off=false"
    else
        jvm_prop+=" -Dcom.ericsson.component.aia.services.eps.core.statistics.off=true"
    fi
}

set_perm_size(){
    local perm_gen_size="-XX:PermSize=64m"
    echo "Sets the perm size"
    memory_prop+=" $perm_gen_size"
}

set_max_permgen_size(){
    local max_perm_gen="-XX:MaxPermSize=64m"
    echo "Sets Max perm size"
    memory_prop+=" $max_perm_gen"
}

set_gc_options(){
    echo "Sets GC options"
    jvm_prop+=" -XX:+UseParallelGC"
    jvm_prop+=" -XX:+UseParallelOldGC"
    jvm_prop+=" -XX:ParallelGCThreads=4"
    jvm_prop+=" -XX:MaxGCPauseMillis=2"
    jvm_prop+=" -XX:MaxHeapFreeRatio=70 "
}

eps_command_add_eps_flow_directory(){
    jvm_prop+=" -Dcom.ericsson.component.aia.services.eps.module.deployment.folder.path=${EPS_FLOW_DIRECTORY}"
}

eps_command_max_heap_size(){
    if [ ! -z "$MAX_HEAP_SIZE_GB" ] ; then
        memory_prop+=" -Xmx${MAX_HEAP_SIZE_GB}"
    fi
}

eps_command_initial_heap_size(){
    if [ ! -z "$INITIAL_HEAP_SIZE_GB" ] ; then
        memory_prop+=" -Xms$INITIAL_HEAP_SIZE_GB"
    fi
}

eps_command_add_eps_lib_files(){
    classpath="${DEFAULT_CLASSPATH}"
    if [ ! -z "$EPS_LIB_FILES" ] ; then
        classpath+="$EPS_LIB_FILES"
    fi
}

eps_command_add_eps_instrumentation(){
    if [ ! -z "$INSTRUMENTATION_METHOD" ] ; then
       jvm_prop+=" -Dcom.ericsson.component.aia.services.eps.core.statistics.reporting.method=$INSTRUMENTATION_METHOD"
       jvm_prop+=" -Dcom.ericsson.component.aia.services.eps.core.statistics.reporting.csv.location=$INSTRUMENTATION_LOCATION "
    fi
}

start_eps_process(){
    echo ${JAVA} -cp $classpath $memory_prop $GC $jvm_prop $jmx_prop $DEFAULT_MAIN_CLASS_NAME
    ${JAVA} -cp $classpath $memory_prop $GC $jvm_prop $jmx_prop $DEFAULT_MAIN_CLASS_NAME
}

set_system_property(){
    jvm_prop+=" -Dio.netty.allocator.type=\"pooled\""
    jvm_prop+=" -DschemaRegistry.address=${SCHEMA_REGISTRY}"
}

start_eps(){
    echo "Starting an EPS instance called '$EPS_INSTANCE_ID'"
    $ECHO "Starting $EPS_INSTANCE_ID:"
    set_eps_jvm
    set_jmx_tag
    set_system_property
    #This value needs to be in the first 1024 characters for DDC to pick up JMX data
    eps_command_add_eps_flow_directory
    eps_command_max_heap_size
    eps_command_initial_heap_size
    eps_command_add_eps_lib_files
    eps_command_add_eps_instrumentation
    set_gc_options
    set_perm_size
    set_max_permgen_size
    start_eps_process
}


delete_directory(){
    local directory=$1

    if [ ! -d $directory ] ; then
        echo "Deleting directory ${directory}"
        $RM -r $directory
        if [ $? -ne 0 ]
        then
            echo ${directory}
            echo "Cannot delete directory ${directory}"
            exit 10
        fi
    fi
}

#####################
#####################
### Main Function ###
#####################
#####################

check_memory_on_kvm

create_directory $EPS_FLOW_DIRECTORY
create_directory $APEPS_LOG_DIR
create_directory $INSTRUMENTATION_LOCATION
create_directory $EPS_IPL_DIRECTORY
copyMountedFilesFromStagingToFlowAndIplLocations
updateIPLGroupIDWithHostId

start_eps