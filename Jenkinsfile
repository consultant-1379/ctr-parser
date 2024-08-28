@Library('son-dev-utils-shared-library')
import jenkins.utils.*

logging = new logging() // https://gerrit.ericsson.se/gitweb?p=OSS/com.ericsson.oss.services.sonom/son-dev-utils.git;a=blob_plain;f=src/jenkins/utils/logging.groovy;hb=master
utils = new utils()     // https://gerrit.ericsson.se/gitweb?p=OSS/com.ericsson.oss.services.sonom/son-dev-utils.git;a=blob_plain;f=src/jenkins/utils/utils.groovy;hb=master

pipeline {
    agent {
        node {
            label SLAVE
        }
    }
    options {
        skipDefaultCheckout true
        timestamps()
        timeout(time: 90, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '40', artifactNumToKeepStr: '20'))
    }
    environment {
        SERVICE_NAME = "eric-event-data-collector"
        SERVICE_NAME_ADC = "eric-oss-5gpmevent-filetrans-proc"
        CREDENTIALS_SEKA_ARTIFACTORY = credentials('ejenksonomArtifactoryApiKey')
        CREDENTIALS_SEKI_ARTIFACTORY = credentials('ejenksonomArtifactoryApiKeySEKI')
        CREDENTIALS_SQAPITOKEN_ECSON = credentials ('SQApiToken-ECSON')
        HELM_CHART_PACKAGED = "${WORKSPACE}/.bob/$SERVICE_NAME-internal/*.tgz"
        ADC_HELM_CHART_PACKAGED = "${WORKSPACE}/.bob/${SERVICE_NAME_ADC}-internal/*.tgz"
        HELM_CHART_REPO = "proj-ec-son-ci-internal"
        HELM_CHART_DROP_REPO = "https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm"
        HELM_INSTALL_RELEASE_NAME = "${SERVICE_NAME}-install"
        HELM_INSTALL_RELEASE_NAME_ADC = "${SERVICE_NAME_ADC}-install"
        HELM_INSTALL_RELEASE_NAME_INT = "${SERVICE_NAME_ADC}-integration"
        HELM_INSTALL_NAMESPACE = "${HELM_INSTALL_RELEASE_NAME}"
        HELM3_INSTALL_TIMEOUT = "900s"

        HELM_INT_CHART_DIRECTORY = "${WORKSPACE}/${SERVICE_NAME_ADC}/charts/${SERVICE_NAME_ADC}-integration"

        HELM_SET = "images.eric-oss-5gpmevent-filetrans-proc.name=${SERVICE_NAME_ADC},imageCredentials.eric-oss-5gpmevent-filetrans-proc.repoPath=${HELM_CHART_REPO},imageCredentials.eric-oss-5gpmevent-filetrans-procTest.repoPath=${HELM_CHART_REPO},imageCredentials.eric-oss-5gpmevent-filetrans-procTest.name=${SERVICE_NAME_ADC}-integration,celltrace.persistentVolumeClaim.enabled=true,celltrace.persistentVolumeClaim.claimName=${SERVICE_NAME_ADC}-pvc,celltrace.persistentVolumeClaim.mountPath='/tmp/files',global.pullSecret=${SERVICE_NAME}-secret,imageCredentials.pullSecret=${SERVICE_NAME}-secret "
        HELM_SET_INT = "testsuite.image.repository=armdocker.rnd.ericsson.se/${HELM_CHART_REPO},testsuite.image.name=${HELM_INSTALL_RELEASE_NAME_INT},testsuite.image.deployment=${HELM_INSTALL_RELEASE_NAME_INT},global.pullSecret=${SERVICE_NAME}-secret,imageCredentials.pullSecret=${SERVICE_NAME}-secret,imageCredentials.repository=armdocker.rnd.ericsson.se/${HELM_CHART_REPO}"
        BOB = "docker run --rm \
            --env APP_PATH=${WORKSPACE} \
            --env ADP_HELM_DR_CHECK_SKIPPED_RULES=${ADP_HELM_DR_CHECK_SKIPPED_RULES} \
            --env ADP_HELM_DR_CHECK_TAG=${ADP_HELM_DR_CHECK_TAG} \
            --env ADP_RELEASE_AUTO_TAG=${ADP_RELEASE_AUTO_TAG} \
            --env DOC_BUILDER_TAG=${DOC_BUILDER_TAG} \
            --env RAML_BUILDER_TAG=${RAML_BUILDER_TAG} \
            -v ${WORKSPACE}:${WORKSPACE} \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -w ${WORKSPACE} \
            ${BOB_DOCKER_IMAGE}"
        HELM3_CMD = "docker run --rm \
            -v ${WORKSPACE}/.kube/config:/root/.kube/config \
            -v ${WORKSPACE}/.config/helm:/root/.config/helm \
            -v ${WORKSPACE}/.local/share/helm:/root/.local/share/helm \
            -v ${WORKSPACE}/.cache/helm:/root/.cache/helm \
            -v ${WORKSPACE}:${WORKSPACE} \
            ${HELM3_DOCKER_IMAGE}"
        KUBECTL_CMD = "docker run --rm \
            -v ${WORKSPACE}/.kube/config:/root/.kube/config \
            -v ${WORKSPACE}:${WORKSPACE} \
            ${KUBECTL_DOCKER_IMAGE}"

        DOCKER_MVN_NPM_BUILDER="docker run --rm \
                                --user lciadm100 \
                                -w ${WORKSPACE} \
                                --env SONAR_AUTH_TOKEN=${CREDENTIALS_SQAPITOKEN_ECSON_PSW} \
                                --env GERRIT_CHANGE_NUMBER=${GERRIT_CHANGE_NUMBER} \
                                -v ${WORKSPACE}:${WORKSPACE} \
                                -v /home/lciadm100/.m2:${HOME}/.m2 \
                                -v /home/lciadm100/.ssh:${HOME}/.ssh \
                                -v /home/lciadm100/.gitconfig:${HOME}/.gitconfig \
                                   armdocker.rnd.ericsson.se/proj-ec-son-dev/mvn-npm-builder:jdk11-jdk8-lci7 bash -c"
    }
    stages {
        stage('Clean') {
            steps {
                echo "Cleanup workspace"
                cleanWs()
                echo 'SCM Checkout'
                checkout scm
            }
        }
        stage('Init') {
            steps {
                script {
                    utils.injectFiles()
                    //sh 'git rm --cached bob'
                    sh 'git submodule sync'
                    sh 'git submodule update --init --recursive'

                    sh '''
                       echo 'Prepare Helm3'

                       ${HELM3_CMD} repo add ${SERVICE_NAME_ADC}-repo ${HELM_CHART_DROP_REPO} --username ${CREDENTIALS_SEKA_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKA_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add ${SERVICE_NAME_ADC}-drop-repo ${HELM_CHART_DROP_REPO} --username ${CREDENTIALS_SEKA_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKA_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add eric-data-coordinator-zk "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/" --username ${CREDENTIALS_SEKI_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKI_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add eric-data-message-bus-kf "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-all-helm/" --username ${CREDENTIALS_SEKI_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKI_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add eric-data-document-database-pg "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-gs-released-helm" --username ${CREDENTIALS_SEKI_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKI_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add eric-schema-registry-sr "https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm/" --username ${CREDENTIALS_SEKA_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKA_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add eric-data-engine-sk "https://arm.epk.ericsson.se/artifactory/proj-ec-son-drop-helm" --username ${CREDENTIALS_SEKA_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKA_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add eric-pm-server "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-pm-server-helm" --username ${CREDENTIALS_SEKI_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKI_ARTIFACTORY_PSW}
                       ${HELM3_CMD} repo add eric-cm-mediator "https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-adp-cm-mediator-helm" --username ${CREDENTIALS_SEKI_ARTIFACTORY_USR} --password ${CREDENTIALS_SEKI_ARTIFACTORY_PSW}

                       ${HELM3_CMD} repo update
                        '''
                    if (env.K8S == "true") {
                        def releasesToClean =      [HELM_INSTALL_RELEASE_NAME, HELM_INSTALL_RELEASE_NAME_INT]
                        def namespacesOfReleases = [HELM_INSTALL_NAMESPACE,    HELM_INSTALL_NAMESPACE]
                        utils.cleanupHelm3ReleasesNamespaces(releasesToClean, namespacesOfReleases)
                    }
                }
            }
        }
	stage('Dependency Updates') {
            when {
                expression { env.TRIGGERED_BY_STAGING == "true" }
            }
            steps {
                script {
                    utils.updateDependencyWhenTriggeredByStaging()
                }
            }
        }
        stage('Build') {
             steps {
                script {
                    sh "${DOCKER_MVN_NPM_BUILDER} 'mvn clean install -f ./eric-oss-5gpmevent-filetrans-proc/pom.xml -s aia_settings.xml -DskipTests=true' > mvn-build-adc.log"
                    sh "mvn clean --settings aia_settings.xml -f ./Docker/pom.xml -U -V -B -Pbuild-docker-image package > mvn-build.log"
                    sh "mvn clean install -f ./testsuite/pom.xml -U -V -B --settings aia_settings.xml"

                    sh 'git update-index --assume-unchanged Docker/buildNumber.properties'
                }
            }
        }
        stage('Image') {
            steps {
                script {
                    if (env.RELEASE == "true") {
                        sh "${BOB} init-drop > bob-init.log"
                    } else {
                        sh "${BOB} init-review > bob-init.log"
                    }
                }
                sh "sudo chmod -fR 777 .bob/"
                sh 'echo "${CREDENTIALS_SEKA_ARTIFACTORY_PSW}" > .bob/var.HELM_REPO_API_TOKEN'
                sh "${BOB} image > bob-image.log"
            }
        }
        stage('Package') {
            steps {
                script {
                    if (env.RELEASE == "true" || env.K8S == "true") {
                        sh "${BOB} package > bob-package.log"
                    } else {
                        sh "${BOB} package-local:package-helm-internal > bob-package-helm.log"
                        sh "${BOB} package-local:image-push-internal > bob-package-image.log"
                        sh "${BOB} package:helm-upload-internal > bob-package-upload.log"
                    }
                }
            }
        }
        stage('Lint') {
            steps {
            script {
                    echo 'Lint and Helm Design Rules Check:'
                    sh "${BOB} lint > bob-lint.log"

                    echo 'Execute Helm template and Dry Run Install...'
                    sh '${HELM3_CMD} template ${HELM_CHART_PACKAGED} > helm-template.log'
                    sh '${HELM3_CMD} install ${HELM_INSTALL_RELEASE_NAME} ${HELM_CHART_PACKAGED} --debug --dry-run > helm-install-dry-run.log'
                }
            }
        }
        stage('TestDeploy') {
            when {
                expression { env.PRE_CODE_REVIEW == "true"  }
            }
            steps {
                sh "${KUBECTL_CMD} create ns testedc"
                sh "${KUBECTL_CMD} create secret docker-registry testdeploy-secret \
                    --docker-server=armdocker.rnd.ericsson.se \
                    --docker-username=${CREDENTIALS_SEKA_ARTIFACTORY_USR} \
                    --docker-password=${CREDENTIALS_SEKA_ARTIFACTORY_PSW} \
                    -n testedc || true"

                sh 'echo "${CREDENTIALS_SEKA_ARTIFACTORY_PSW}" > .bob/var.CREDENTIALS_SEKA_ARTIFACTORY_PSW'
                sh 'echo "${CREDENTIALS_SEKI_ARTIFACTORY_PSW}" > .bob/var.CREDENTIALS_SEKI_ARTIFACTORY_PSW'
                sh "${BOB} testdeploy"
            }
        }
        stage('K8S install') {
            when {
                expression { env.K8S == "true" }
            }
            steps {
                sh '${HELM3_CMD} template ${ADC_HELM_CHART_PACKAGED} > helm-template.log'
                sh '${HELM3_CMD} install ${HELM_INSTALL_RELEASE_NAME} ${ADC_HELM_CHART_PACKAGED} --set ${HELM_SET} --set images.eric-oss-5gpmevent-filetrans-proc.tag="$(cat .bob/var.version)" --set imageCredentials.eric-oss-5gpmevent-filetrans-procTest.tag="$(cat .bob/var.version)" --debug --dry-run > helm-install-dry-run.log'

                echo 'Initial install of the service helm chart:'
                sh "${KUBECTL_CMD} create ns ${HELM_INSTALL_NAMESPACE} || true"
                sh "${KUBECTL_CMD} create secret docker-registry ${SERVICE_NAME}-secret \
                    --docker-server=armdocker.rnd.ericsson.se \
                    --docker-username=${CREDENTIALS_SEKA_ARTIFACTORY_USR} \
                    --docker-password=${CREDENTIALS_SEKA_ARTIFACTORY_PSW} \
                    -n ${HELM_INSTALL_NAMESPACE} || true"
                sh '${HELM3_CMD} upgrade \
                    --install ${HELM_INSTALL_RELEASE_NAME_ADC} ${ADC_HELM_CHART_PACKAGED} \
                    --set ${HELM_SET} \
                    --set images.eric-oss-5gpmevent-filetrans-proc.tag="$(cat .bob/var.version)" \
                    --set imageCredentials.eric-oss-5gpmevent-filetrans-procTest.tag="$(cat .bob/var.version)" \
                    --set imageCredentials.pullSecret=${SERVICE_NAME}-secret \
                    --set global.pullSecret=${SERVICE_NAME}-secret \
                    --namespace ${HELM_INSTALL_NAMESPACE} \
                    --timeout ${HELM3_INSTALL_TIMEOUT} \
                    --devel \
                    --wait'
            }
        }
        stage('K8S test') {
            when {
                expression { env.K8S == "true" }
            }
            steps {
                echo 'Update Helm integration chart dependencies:'
                sh '${HELM3_CMD} dependency update ${HELM_INT_CHART_DIRECTORY}'
                echo 'Install the Helm integration chart:'
                sh '${HELM3_CMD} install \
                    ${HELM_INSTALL_RELEASE_NAME_INT} ${HELM_INT_CHART_DIRECTORY} \
                    --set ${HELM_SET_INT} \
                    --set imageCredentials.pullSecret=${SERVICE_NAME}-secret \
                    --set global.pullSecret=${SERVICE_NAME}-secret \
                    --set testsuite.image.tag="$(cat .bob/var.version)" \
                    --namespace ${HELM_INSTALL_NAMESPACE} \
                    --timeout ${HELM3_INSTALL_TIMEOUT} \
                    --wait'
                echo 'Run the basic helm test'
                sh '${HELM3_CMD} test ${HELM_INSTALL_RELEASE_NAME_ADC} --namespace ${HELM_INSTALL_NAMESPACE} --debug --timeout ${HELM3_INSTALL_TIMEOUT}'
                sh '${HELM3_CMD} test ${HELM_INSTALL_RELEASE_NAME_INT} --namespace ${HELM_INSTALL_NAMESPACE} --debug --timeout ${HELM3_INSTALL_TIMEOUT}'
            }
        }
        stage('Dependency Updates Commit') {
            when {
                expression { env.TRIGGERED_BY_STAGING == "true" }
            }
            steps {
                script {
                    utils.commitChangesWhenTriggeredByStaging()
                }
            }
        }
	stage('Publish') {
            when {
                expression { env.RELEASE == "true"  }
            }
            steps {
                sh "${BOB} publish > bob-publish.log"
                archiveArtifacts 'artifact.properties'
            }
        }
    }
    post {
        always {
            script {
                utils.postAlways()
                if (env.K8S  == "true") {
                    logging.get_logs_for_each_namespace(HELM_INSTALL_NAMESPACE)

                    def releasesToClean =      [HELM_INSTALL_RELEASE_NAME, HELM_INSTALL_RELEASE_NAME_INT]
                    def namespacesOfReleases = [HELM_INSTALL_NAMESPACE,    HELM_INSTALL_NAMESPACE]
                    utils.cleanupHelm3ReleasesNamespaces(releasesToClean, namespacesOfReleases)
                }
            }
        }
        failure {
            script {
                utils.postFailure()
            }
        }
        success {
            script {
                utils.postSuccess()
                utils.modifyBuildDescription("eric-event-data-collector")
            }
        }
    }
}
