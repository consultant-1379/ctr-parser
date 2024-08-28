ARG CBO_VERSION="6.7.0-9"
FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles:${CBO_VERSION}

RUN echo "102825:x:102825:102825:An Identity for Event-Data-Collector:/usr/sbin/:/bin/false" >> /etc/passwd \
    && echo "102825:!::0:::::" >> /etc/shadow

ARG CBOS_VERSION="6.7.0-9"
ARG CBO_REPO_URL=https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${CBOS_VERSION}
ARG SUSE_PKG_REPO_URL=http://download.opensuse.org/distribution/leap/15.0/repo/oss/

LABEL GIT_COMMIT=unknown

ENV SCHEMA_REGISTRY=http://schemaRegistry:8081 \
    APPLICATION_ID=apeps
ENV APPLICATION_HOME=/ericsson/${APPLICATION_ID}

ENV FLOW_DIRECTORY=${APPLICATION_HOME}/flow

#reporting available option: CSV, JMX, SLF4J
ENV INSTRUMENTATION_ON=true \
    INSTRUMENTATION_METHOD=CSV \
    INSTRUMENTATION_FILE_LOCATION=${APPLICATION_HOME}/reporting \
    JMX_PORT=21000 \
    LOGBACK_FILE=${APPLICATION_HOME}/etc/logback.xml \
    LOG_DIR=${APPLICATION_HOME}/log \
    MAX_HEAP_SIZE_GB="1500M" \
    INITIAL_HEAP_SIZE_GB="1500M"

COPY Docker/staging /
COPY Docker/conf/scripts/docker-entrypoint.sh /usr/sbin/

RUN chown -R 102825:0 /ericsson \
    && chmod -R g=u /ericsson \
    && chmod 777 /usr/sbin/docker-entrypoint.sh

RUN zypper addrepo -C -G -f ${CBO_REPO_URL}?ssl_verify=no LDC-CBO-SLES \
    && zypper ref -f -r LDC-CBO-SLES \
    && zypper ar -C -G -f ${SUSE_PKG_REPO_URL}?ssl_verify=no SUSE-REPO \
    && zypper ref -f -r SUSE-REPO \
    && zypper install -l -y java-1_8_0-openjdk-headless hostname jq curl zip\
    # Remove JMSAppender, JMSSink, JDBCAppender, Chainsaw from log4j jar to mitigate CVE-2021-4104, CVE-2022-23302, CVE-2022-23305 & CVE-2022-23307
    && zip -q -d /ericsson/apeps/lib/log4j-*.jar  \
    org/apache/log4j/net/JMSAppender.class org/apache/log4j/net/JMSSink.class org/apache/log4j/jdbc/JDBCAppender.class org/apache/log4j/chainsaw/* \
    && zypper --non-interactive rm zip \
    && rm -rf /files \
    && zypper clean --all \
    && zypper rr SUSE-REPO \
    && zypper rr LDC-CBO-SLES

EXPOSE 21000-21010

USER 102825

CMD     ["/usr/sbin/docker-entrypoint.sh"]