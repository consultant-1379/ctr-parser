ARG CBO_VERSION="6.7.0-9"
FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles:${CBO_VERSION}

ARG CBOS_VERSION="6.7.0-9"
ARG CBO_REPO_URL=https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBOS_VERSION}

#Install openJDK 8
ENV JAVA_VERSION=1.8.0_222 \
    JAVA_HOME=/usr/

RUN zypper addrepo -C -G -f ${CBO_REPO_URL}?ssl_verify=no \
    COMMON_BASE_OS_SLES_REPO \
    && zypper install -l -y java-1_8_0-openjdk-headless curl \
    && zypper clean --all \
    && zypper rr COMMON_BASE_OS_SLES_REPO

ENV KAFKA_BOOTSTRAP_SERVER=eric-pm-events-message-bus-kf:9092 \
    KAFKA_INPUT_TOPIC_NAME=celltraceInputFiles5G \
    KAFKA_OUTPUT_TOPIC_NAME=celltraceDecoded5G \
    KAFKA_GROUP_ID=group_id

RUN mkdir -p /opt/event-data-collector-testsuite
RUN mkdir -p /opt/event-data-collector-testsuite/ctrfiles
COPY ./target/testsuite-uber-jar.jar /opt/event-data-collector-testsuite
COPY ./src/test/resources/test-data/*.* /opt/event-data-collector-testsuite/ctrfiles/
ENTRYPOINT [ "java","-jar","/opt/event-data-collector-testsuite/testsuite-uber-jar.jar" ]