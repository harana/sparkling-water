#!/usr/bin/groovy

def call(params, body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST

    body.delegate = config
    body(params)

    def customEnv = [
            "SPARK=spark-${config.sparkVersion}-bin-hadoop${config.hadoopVersion}",
            "SPARK_HOME=${env.WORKSPACE}/spark",
            "HADOOP_CONF_DIR=/etc/hadoop/conf",
            "MASTER=yarn-client",
            "H2O_EXTENDED_JAR=${env.WORKSPACE}/assembly-h2o/private/extended/h2odriver-extended.jar",
            // Properties used in case we are building against specific H2O version
            "BUILD_HADOOP=true",
            "H2O_TARGET=${config.driverHadoopVersion}",
            "H2O_ORIGINAL_JAR=${env.WORKSPACE}/h2o-3/h2o-hadoop-2/h2o-${config.driverHadoopVersion}-assembly/build/libs/h2odriver.jar"
    ]

    ansiColor('xterm') {
        timestamps {
            withEnv(customEnv) {
                timeout(time: 180, unit: 'MINUTES') {
                    dir("${env.WORKSPACE}") {
                        prepareSparkEnvironment()(config)
                        prepareSparklingWaterEnvironment()(config)
                        buildAndLint()(config)
                        unitTests()(config)
                        pyUnitTests()(config)
                        rUnitTests()(config)
                        localIntegTest()(config)
                        localPyIntegTest()(config)
                        scriptsTest()(config)
                        // Run Integration tests on YARN
                        node("dX-hadoop") {
                            def customEnvNew = [
                                    "SPARK=spark-${config.sparkVersion}-bin-hadoop${config.hadoopVersion}",
                                    "SPARK_HOME=${env.WORKSPACE}/spark",
                                    "HADOOP_CONF_DIR=/etc/hadoop/conf",
                                    "MASTER=yarn-client",
                                    "H2O_EXTENDED_JAR=${env.WORKSPACE}/assembly-h2o/private/extended/h2odriver-extended.jar",
                                    "JAVA_HOME=/usr/lib/jvm/java-8-oracle/",
                                    "PATH=/usr/lib/jvm/java-8-oracle/bin:${PATH}",
                                    // Properties used in case we are building against specific H2O version
                                    "BUILD_HADOOP=true",
                                    "H2O_TARGET=${config.driverHadoopVersion}",
                                    "H2O_ORIGINAL_JAR=${env.WORKSPACE}/h2o-3/h2o-hadoop-2/h2o-${config.driverHadoopVersion}-assembly/build/libs/h2odriver.jar"
                            ]
                            withEnv(customEnvNew) {
                                integTest()(config)
                            }
                        }
                        pysparklingIntegTest()(config)
                        publishNightly()(config)
                    }
                }
            }
        }
    }
}

def withDocker(config, code) {
    def image = 'opsh2oai/sparkling_water_tests:' + config.dockerVersion
    retryWithDelay(3, 120,{
        withCredentials([usernamePassword(credentialsId: "harbor.h2o.ai", usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD')]) {
            sh "docker login -u $REGISTRY_USERNAME -p $REGISTRY_PASSWORD harbor.h2o.ai"
            sh "docker pull harbor.h2o.ai/${image}"
        }
    })
    docker.image(image).inside("--init --privileged --dns 172.16.0.200 -v /home/0xdiag:/home/0xdiag") {
        sh "activate_java_8"
        code()
    }
}


def getGradleCommand(config) {
    def gradleStr
    if (config.buildAgainstH2OBranch.toBoolean()) {
        gradleStr = "H2O_HOME=${env.WORKSPACE}/h2o-3 ${env.WORKSPACE}/gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --include-build ${env.WORKSPACE}/h2o-3"
    } else {
        gradleStr = "${env.WORKSPACE}/gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false"
    }

    if (config.buildAgainstSparkBranch.toBoolean()) {
        "${gradleStr} -x checkSparkVersionTask -PtestMojoPipeline=true -PsparkVersion=${config.sparkVersion}"
    } else {
        "${gradleStr} -PtestMojoPipeline=true"
    }
}


def prepareSparkEnvironment() {
    return { config ->
        stage('Prepare Spark Environment - ' + config.backendMode) {
            withDocker(config) {
                if (config.buildAgainstSparkBranch.toBoolean()) {
                    // build spark
                    sh """
                    git clone https://github.com/apache/spark.git spark_repo
                    cd spark_repo
                    git checkout ${config.sparkBranch}
                    ./dev/make-distribution.sh --name custom-spark --pip -Phadoop-${config.hadoopVersion} -Pyarn -Phive
                    cp -r ./dist/ ${env.SPARK_HOME}
                    """
                } else {
                    sh  """
                        cp -R \${SPARK_HOME_2_4} ${env.SPARK_HOME}
                        """
                }

                sh """
                # Setup Spark
                echo "spark.driver.extraJavaOptions -Dhdp.version="${config.hdpVersion}"" >> ${env.SPARK_HOME}/conf/spark-defaults.conf
                echo "spark.yarn.am.extraJavaOptions -Dhdp.version="${config.hdpVersion}"" >> ${env.SPARK_HOME}/conf/spark-defaults.conf
                echo "spark.executor.extraJavaOptions -Dhdp.version="${config.hdpVersion}"" >> ${env.SPARK_HOME}/conf/spark-defaults.conf
                
                echo "-Dhdp.version="${config.hdpVersion}"" >> ${env.SPARK_HOME}/conf/java-opts
                """
                if (config.buildAgainstSparkBranch.toBoolean()) {
                    sh """
                    echo "spark.ext.h2o.spark.version.check.enabled false" >> ${env.SPARK_HOME}/conf/spark-defaults.conf
                    """
                }
            }
        }
    }
}


def prepareSparklingWaterEnvironment() {
    return { config ->
        stage('QA: Prepare Sparkling Water Environment - ' + config.backendMode) {
            withDocker(config) {
                // In case of building against nightly build, modify gradle.properties
                // We can however can do nightly based on specific H2O branch, in that case this needs to be skipped
                if (config.buildNightly.toBoolean() && !config.buildAgainstH2OBranch.toBoolean()) {

                    def h2oNightlyBuildVersion = new URL("http://h2o-release.s3.amazonaws.com/h2o/master/latest").getText().trim()

                    def h2oNightlyMajorVersion = new URL("http://h2o-release.s3.amazonaws.com/h2o/master/${h2oNightlyBuildVersion}/project_version").getText().trim()
                    h2oNightlyMajorVersion = h2oNightlyMajorVersion.substring(0, h2oNightlyMajorVersion.lastIndexOf('.'))

                    sh """
                     REL_VERSION=`cat gradle.properties | grep version | grep -v '#' | sed -e "s/.*=//"`
                     BUILD_VERSION=\$(wget https://h2o-release.s3.amazonaws.com/sparkling-water/${BRANCH_NAME}/nightly/latest -q -O -)

                     NEW_BUILD_VERSION=\$((\${BUILD_VERSION} + 1))
                     NEW_REL_VERSION=\${REL_VERSION}-\${NEW_BUILD_VERSION}

                     sed -i.backup -E "s/h2oMajorName=.*/h2oMajorName=master/" gradle.properties
                     sed -i.backup -E "s/h2oMajorVersion=.*/h2oMajorVersion=${h2oNightlyMajorVersion}/" gradle.properties
                     sed -i.backup -E "s/h2oBuild=.*/h2oBuild=${h2oNightlyBuildVersion}/" gradle.properties
                     sed -i.backup -E "s/\${REL_VERSION}/\${NEW_REL_VERSION}/" gradle.properties
                    """
                }

                sh """
                # Check if we are bulding against specific H2O branch
                if [ ${config.buildAgainstH2OBranch} = true ]; then
                    # Clone H2O
                    git clone https://github.com/h2oai/h2o-3.git
                    cd h2o-3
                    git checkout ${config.h2oBranch}
                    . /envs/h2o_env_python2.7/bin/activate
                    ./gradlew build -x check -x :h2o-r:build
                    cd ..
                    if [ ${config.backendMode} = external ]; then
                        # In this case, PySparkling build is driven by H2O_HOME property
                        # When extending from specific jar the jar has already the desired name
                        ${getGradleCommand(config)} -q :sparkling-water-examples:build -x check -PdoExtend extendJar
                    fi
                else if [ ${config.backendMode} = external ]; then
                        cp `${getGradleCommand(config)} -q :sparkling-water-examples:build -x check -PdoExtend extendJar -PdownloadH2O=${config.driverHadoopVersion}` ${env.H2O_EXTENDED_JAR}
                     fi
                fi
    
                """
            }
        }
    }
}


def buildAndLint() {
    return { config ->
        stage('QA: Build and Lint - ' + config.backendMode) {
            withDocker(config) {
                withCredentials([usernamePassword(credentialsId: "LOCAL_NEXUS", usernameVariable: 'LOCAL_NEXUS_USERNAME', passwordVariable: 'LOCAL_NEXUS_PASSWORD')]) {
                    sh """
                    # Build
                    ${getGradleCommand(config)} clean build -x check scalaStyle -PlocalNexusUsername=$LOCAL_NEXUS_USERNAME -PlocalNexusPassword=$LOCAL_NEXUS_PASSWORD
                    """

                    stash 'sw-build'
                }
            }
        }
    }
}

def unitTests() {
    return { config ->
        stage('QA: Unit Tests - ' + config.backendMode) {
            withDocker(config) {
                if (config.runUnitTests.toBoolean()) {
                    try {
                        withCredentials([string(credentialsId: "DRIVERLESS_AI_LICENSE_KEY", variable: "DRIVERLESS_AI_LICENSE_KEY")]) {
                            if(config.backendMode == "external"){
                                sh "sudo -E /usr/sbin/startup.sh"
                            }
                            sh """
                            # Run unit tests
                            ${getGradleCommand(config)} test -x :sparkling-water-r:test -x :sparkling-water-py:test -x integTest -PbackendMode=${config.backendMode} -PexternalBackendStartMode=auto
                            """
                        }
                    } finally {
                        arch '**/build/*tests.log,**/*.log, **/out.*, **/*py.out.txt, **/stdout, **/stderr, **/build/**/*log*, py/build/py_*_report.txt, **/build/reports/'
                        junit 'core/build/test-results/test/*.xml'
                        junit 'ml/build/test-results/test/*.xml'
                        testReport 'core/build/reports/tests/test', 'Core Unit tests'
                        testReport 'ml/build/reports/tests/test', "ML Unit Tests"
                    }

                }
            }
        }

    }
}

def pyUnitTests() {
    return { config ->

        stage('QA: Python Unit Tests 3.6 - ' + config.backendMode) {
            withDocker(config) {
                if (config.runPyUnitTests.toBoolean()) {
                    try {
                        withCredentials([string(credentialsId: "DRIVERLESS_AI_LICENSE_KEY", variable: "DRIVERLESS_AI_LICENSE_KEY")]) {
                            if(config.backendMode == "external"){
                                sh "sudo -E /usr/sbin/startup.sh"
                            }
                            sh """
                            ${getGradleCommand(config)} :sparkling-water-py:test -PpythonPath=/envs/h2o_env_python3.6/bin -PpythonEnvBasePath=/home/jenkins/.gradle/python -x integTest -PbackendMode=${config.backendMode}
                            """
                        }
                    } finally {
                        arch '**/build/*tests.log,**/*.log, **/out.*, **/*py.out.txt, **/stdout, **/stderr, **/build/**/*log*, py/build/py_*_report.txt, **/build/reports/'
                    }

                }
            }
        }

        stage('QA: Python Unit Tests 2.7 - ' + config.backendMode) {
            withDocker(config) {
                if (config.runPyUnitTests.toBoolean()) {
                    try {
                        withCredentials([string(credentialsId: "DRIVERLESS_AI_LICENSE_KEY", variable: "DRIVERLESS_AI_LICENSE_KEY")]) {
                            if(config.backendMode == "external"){
                                sh "sudo -E /usr/sbin/startup.sh"
                            }
                            sh """
                            ${getGradleCommand(config)} :sparkling-water-py:test -PpythonPath=/envs/h2o_env_python2.7/bin -PpythonEnvBasePath=/home/jenkins/.gradle/python -x integTest -PbackendMode=${config.backendMode}
                            """
                        }
                    } finally {
                        arch '**/build/*tests.log,**/*.log, **/out.*, **/*py.out.txt, **/stdout, **/stderr, **/build/**/*log*, py/build/py_*_report.txt, **/build/reports/'
                    }

                }
            }
        }

    }
}

def rUnitTests() {
    return { config ->
        stage('QA: RUnit Tests - ' + config.backendMode) {
            withDocker(config) {
                if (config.runRUnitTests.toBoolean()) {
                    try {
                          sh "sudo -E /usr/sbin/startup.sh"
                          sh """
                             ${getGradleCommand(config)} :sparkling-water-r:installH2ORPackage :sparkling-water-r:installRSparklingPackage
                             ${getGradleCommand(config)} :sparkling-water-r:test -x check -PbackendMode=${config.backendMode} -PexternalBackendStartMode=auto
                             """
                    } finally {
                        arch '**/build/*tests.log,**/*.log, **/out.*, **/*py.out.txt, **/stdout, **/stderr, **/build/**/*log*, py/build/py_*_report.txt, **/build/reports/'
                    }

                }
            }
        }

    }
}

def localIntegTest() {
    return { config ->
        stage('QA: Local Integration Tests - ' + config.backendMode) {
            withDocker(config) {
                if (config.runLocalIntegTests.toBoolean()) {
                    try {
                        if(config.backendMode == "external"){
                            sh "sudo -E /usr/sbin/startup.sh"
                        }
                        sh """
                        # Run local integration tests
                        ${getGradleCommand(config)} integTest -x :sparkling-water-py:integTest -PsparkHome=${env.SPARK_HOME} -PbackendMode=${config.backendMode} -PexternalBackendStartMode=auto
                        """
                    } finally {
                        arch '**/build/*tests.log, **/*.log, **/out.*, **/*py.out.txt, examples/build/test-results/binary/integTest/*, **/stdout, **/stderr,**/build/**/*log*, py/build/py_*_report.txt,**/build/reports/'
                        junit 'examples/build/test-results/integTest/*.xml'
                        testReport 'core/build/reports/tests/integTest', 'Local Core Integration tests'
                        testReport 'examples/build/reports/tests/integTest', 'Local Integration tests'
                    }
                }
            }
        }
    }
}

def localPyIntegTest() {
    return { config ->

        stage('QA: Local Py Integration Tests 3.6 - ' + config.backendMode) {
            withDocker(config) {
                if (config.runLocalPyIntegTests.toBoolean()) {
                    try {
                        sh "sudo -E /usr/sbin/startup.sh"
                        sh """
                        # Run local integration tests
                        ${getGradleCommand(config)} sparkling-water-py:localIntegTestsPython -PpythonPath=/envs/h2o_env_python3.6/bin -PpythonEnvBasePath=/home/jenkins/.gradle/python -PsparkHome=${env.SPARK_HOME} -PbackendMode=${config.backendMode}
                        """
                    } finally {
                        arch '**/build/*tests.log, **/*.log, **/out.*, **/*py.out.txt, examples/build/test-results/binary/integTest/*, **/stdout, **/stderr,**/build/**/*log*, py/build/py_*_report.txt,**/build/reports/'
                    }
                }
            }
        }

        stage('QA: Local Py Integration Tests 2.7 - ' + config.backendMode) {
            withDocker(config) {
                if (config.runLocalPyIntegTests.toBoolean()) {
                    try {
                        sh "sudo -E /usr/sbin/startup.sh"
                        sh """
                        # Run local integration tests
                        ${getGradleCommand(config)} sparkling-water-py:localIntegTestsPython -PpythonPath=/envs/h2o_env_python2.7/bin -PpythonEnvBasePath=/home/jenkins/.gradle/python -PsparkHome=${env.SPARK_HOME} -PbackendMode=${config.backendMode}
                        """
                    } finally {
                        arch '**/build/*tests.log, **/*.log, **/out.*, **/*py.out.txt, examples/build/test-results/binary/integTest/*, **/stdout, **/stderr,**/build/**/*log*, py/build/py_*_report.txt,**/build/reports/'
                    }
                }
            }
        }
    }
}



def scriptsTest() {
    return { config ->
        stage('QA: Script Tests - ' + config.backendMode) {
            withDocker(config) {
                if (config.runScriptTests.toBoolean()) {
                    try {
                        if(config.backendMode == "external"){
                            sh "sudo -E /usr/sbin/startup.sh"
                        }
                        sh """
                        # Run scripts tests
                        ${getGradleCommand(config)} scriptTest -PbackendMode=${config.backendMode} -PexternalBackendStartMode=auto
                        """
                    } finally {
                        arch '**/build/*tests.log,**/*.log, **/out.*, **/*py.out.txt, **/stdout, **/stderr,**/build/**/*log*, **/build/reports/'
                        junit 'examples/build/test-results/scriptsTest/*.xml'
                        testReport 'examples/build/reports/tests/scriptsTest', 'Script Tests'
                    }
                }
            }
        }
    }
}


def integTest() {
    return { config ->
        stage('QA: Integration Tests - ' + config.backendMode) {
            cleanWs()
            unstash 'sw-build'
            if (config.runIntegTests.toBoolean()) {
                try {
                    sh """
                    ${getGradleCommand(config)} integTest -PbackendMode=${config.backendMode} -PexternalBackendStartMode=auto -PsparklingTestEnv=${config.sparklingTestEnv} -PsparkMaster=${env.MASTER} -PsparkHome=${env.SPARK_HOME} -x check -x :sparkling-water-py:integTest
                    #  echo 'Archiving artifacts after Integration test'
                    """
                } finally {
                    arch '**/build/*tests.log, **/*.log, **/out.*, **/*py.out.txt, examples/build/test-results/binary/integTest/*, **/stdout, **/stderr,**/build/**/*log*, py/build/py_*_report.txt,**/build/reports/'
                    junit 'examples/build/test-results/integTest/*.xml'
                    testReport 'examples/build/reports/tests/integTest', "Integration tests"
                }
            }
        }
    }
}

def pysparklingIntegTest() {
    return { config ->
        stage('QA: PySparkling Integration Tests 3.6 HDP 2.2 - ' + config.backendMode) {
            withDocker(config) {
                if (config.runPySparklingIntegTests.toBoolean()) {
                    try {
                        sh """
                         sudo -E /usr/sbin/startup.sh
                         ${getGradleCommand(config)} sparkling-water-py:yarnIntegTestsPython -PpythonPath=/envs/h2o_env_python3.6/bin -PpythonEnvBasePath=/home/jenkins/.gradle/python -PbackendMode=${config.backendMode} -PsparkHome=${env.SPARK_HOME}
                         # echo 'Archiving artifacts after PySparkling Integration test'
                        """
                    } finally {
                        arch '**/build/*tests.log,**/*.log, **/out.*, **/*py.out.txt, **/stdout, **/stderr,**/build/**/*log*, py/build/py_*_report.txt, **/build/reports/'
                    }
                }
            }
        }

        stage('QA: PySparkling Integration Tests 2.7 HDP 2.2 - ' + config.backendMode) {
            withDocker(config) {
                if (config.runPySparklingIntegTests.toBoolean()) {
                    try {
                        sh """
                         sudo -E /usr/sbin/startup.sh
                         ${getGradleCommand(config)} sparkling-water-py:yarnIntegTestsPython -PpythonPath=/envs/h2o_env_python2.7/bin -PpythonEnvBasePath=/home/jenkins/.gradle/python -PbackendMode=${config.backendMode} -PsparkHome=${env.SPARK_HOME}
                         # echo 'Archiving artifacts after PySparkling Integration test'
                        """
                    } finally {
                        arch '**/build/*tests.log,**/*.log, **/out.*, **/*py.out.txt, **/stdout, **/stderr,**/build/**/*log*, py/build/py_*_report.txt, **/build/reports/'
                    }
                }
            }
        }
    }
}

def getUploadPath(config){
    if (config.buildAgainstH2OBranch.toBoolean()){
        config.h2oBranch.replace("/", "_")
    }else{
        "nightly"
    }
}

def publishNightly() {
    return { config ->
        stage('Nightly: Publishing Artifacts to S3 - ' + config.backendMode) {
            withDocker(config) {
                if (config.buildNightly.toBoolean() && config.uploadNightly.toBoolean()) {

                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'AWS S3 Credentials', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {

                    sh  """
                        # echo 'Making distribution'
                        ${getGradleCommand(config)} dist

                        # Upload to S3
                        """

                        def tmpdir = "./buildsparklingwater.tmp"
                        sh  """
                            
                            

                            if [ ${config.buildAgainstH2OBranch} = false ]; then
                                # Regular nightly build
                                BUILD_VERSION=\$(wget https://h2o-release.s3.amazonaws.com/sparkling-water/${BRANCH_NAME}/nightly/latest -q -O -)
                                NEW_BUILD_VERSION=\$((\${BUILD_VERSION} + 1))
                            else
                                NEW_BUILD_VERSION=\$(date '+%Y_%m_%d_%H_%M_%S')
                            fi
                            
                            #
                            # Upload RSparkling Repo
                            #
                            s3cmd --access_key ${AWS_ACCESS_KEY_ID} --secret_key ${AWS_SECRET_ACCESS_KEY} --acl-public put --recursive r/build/repo/src s3://h2o-release/sparkling-water/${BRANCH_NAME}/${getUploadPath(config)}/\${NEW_BUILD_VERSION}/R/
                            
                            #
                            # Publish PySparkling to S3
                            #    
                            cd py/build/pkg
                            python setup.py sdist
                            DIST_FILE_PATH=\$(ls dist/*)
                            DIST_FILE_NAME=\$(basename \$DIST_FILE_PATH)
                            s3cmd --acl-public put dist/\${DIST_FILE_NAME} s3://h2o-release/sparkling-water/${BRANCH_NAME}/${getUploadPath(config)}/\${NEW_BUILD_VERSION}/py/\${DIST_FILE_NAME}/
                            cd ../../../
                            
                            # Publish the output to S3.
                            echo
                            echo PUBLISH
                            echo
                            s3cmd --rexclude='target/classes/*' --acl-public sync ${env.WORKSPACE}/dist/build/dist/ s3://h2o-release/sparkling-water/${BRANCH_NAME}/${getUploadPath(config)}/\${NEW_BUILD_VERSION}/
                            
                            echo EXPLICITLY SET MIME TYPES AS NEEDED
                            list_of_html_files=`find dist/build/dist -name '*.html' | sed 's/dist\\/build\\/dist\\///g'`
                            echo \${list_of_html_files}
                            for f in \${list_of_html_files}
                            do
                                s3cmd --acl-public --mime-type text/html put dist/build/dist/\${f} s3://h2o-release/sparkling-water/${BRANCH_NAME}/${getUploadPath(config)}/\${NEW_BUILD_VERSION}/\${f}
                            done
                            
                            list_of_js_files=`find dist/build/dist -name '*.js' | sed 's/dist\\/build\\/dist\\///g'`
                            echo \${list_of_js_files}
                            for f in \${list_of_js_files}
                            do
                                s3cmd --acl-public --mime-type text/javascript put dist/build/dist/\${f} s3://h2o-release/sparkling-water/${BRANCH_NAME}/${getUploadPath(config)}/\${NEW_BUILD_VERSION}/\${f}
                            done
                            
                            list_of_css_files=`find dist/build/dist -name '*.css' | sed 's/dist\\/build\\/dist\\///g'`
                            echo \${list_of_css_files}
                            for f in \${list_of_css_files}
                            do
                                s3cmd --acl-public --mime-type text/css put dist/build/dist/\${f} s3://h2o-release/sparkling-water/${BRANCH_NAME}/${getUploadPath(config)}/\${NEW_BUILD_VERSION}/\${f}
                            done
                            
                            if [ ${config.buildAgainstH2OBranch} = false ]; then
                                echo UPDATE LATEST POINTER
                                mkdir -p ${tmpdir} 
                                echo \${NEW_BUILD_VERSION} > ${tmpdir}/latest
                                echo "<head>" > ${tmpdir}/latest.html
                                echo "<meta http-equiv=\\"refresh\\" content=\\"0; url=\${NEW_BUILD_VERSION}/index.html\\" />" >> ${tmpdir}/latest.html
                                echo "</head>" >> ${tmpdir}/latest.html
                                s3cmd --acl-public put ${tmpdir}/latest s3://h2o-release/sparkling-water/${BRANCH_NAME}/nightly/latest
                                s3cmd --acl-public put ${tmpdir}/latest.html s3://h2o-release/sparkling-water/${BRANCH_NAME}/nightly/latest.html
                                s3cmd --acl-public put ${tmpdir}/latest.html s3://h2o-release/sparkling-water/${BRANCH_NAME}/nightly/index.html
                            fi                    
                            """
                    }

                    // Update only if we are doing regular nighly build from master and rel branches
                    if (!config.buildAgainstH2OBranch.toBoolean()) {
                        withCredentials([file(credentialsId: 'master-id-rsa', variable: 'ID_RSA_PATH'), file(credentialsId: 'master-gitconfig', variable: 'GITCONFIG_PATH'), string(credentialsId: 'h2o-ops-personal-auth-token', variable: 'GITHUB_TOKEN'), sshUserPrivateKey(credentialsId: 'h2oOpsGitPrivateKey', keyFileVariable: 'SSH_KEY_GITHUB')]) {

                            sh """
                               # Copy keys
                               mkdir -p ~/.ssh
                               cp \${ID_RSA_PATH} ~/.ssh/id_rsa
                               cp \${GITCONFIG_PATH} ~/.gitconfig

cat <<EOF >>  ~/.ssh/config
Host github.com
   HostName github.com
   User git
   IdentityFile \${SSH_KEY_GITHUB}
   IdentitiesOnly yes
EOF

                                ssh-keyscan github.com >> ~/.ssh/known_hosts
                               """
                            // Update the links
                            retryWithDelay(3, 120, {
                            sh """

                                # S3 Already containes incremented version
                                BUILD_VERSION=\$(wget https://h2o-release.s3.amazonaws.com/sparkling-water/${BRANCH_NAME}/nightly/latest -q -O -)

                                git clone git@github.com:h2oai/docs.h2o.ai.git
                                cd docs.h2o.ai/sites-available/
                                sed -i.backup -E "s?http://h2o-release.s3.amazonaws.com/sparkling-water/${BRANCH_NAME}/nightly/[0-9]+/?http://h2o-release.s3.amazonaws.com/sparkling-water/${BRANCH_NAME}/nightly/\${BUILD_VERSION}/?" 000-default.conf
                                git add 000-default.conf
                                git commit -m "Update links of Sparkling Water nighly version on ${BRANCH_NAME} to \${BUILD_VERSION}"
                                git push --set-upstream origin master
                                cd ../..
                                rm -rf docs.h2o.ai
                            """ })

                        }
                    }
                }
            }
        }
    }
}

return this
