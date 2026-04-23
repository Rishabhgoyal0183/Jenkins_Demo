pipeline {
    agent { label 'jenkins_node' }

    options {
        skipDefaultCheckout(true)
    }

    environment {
       JAVA_HOME  = '/usr/local/jdk-21'
       MAVEN_HOME = '/usr/local/maven'
       PATH       = "/usr/local/jdk-21/bin:/usr/local/maven/bin:${env.PATH}"
       PROTECTED_JAR_DIR = '/opt/protected-builds/staging'
    }

    stages {

        // ─────────────────────────────────────────────
        // SKIPPED for main — main uses the protected JAR
        // ─────────────────────────────────────────────

        stage('Checkout') {
            when {
                not { branch 'main' }
            }
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            when {
                not { branch 'main' }
            }
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Run Unit Tests') {
            when {
                not { branch 'main' }
            }
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: false
                }
                failure {
                    echo 'Tests FAILED — stopping pipeline!'
                    mail to: 'awstesting0183@gmail.com',
                         subject: "TESTS FAILED — ${env.JOB_NAME} [${env.BRANCH_NAME}]",
                         body: """
                            Test stage failed on branch: ${env.BRANCH_NAME}
                            Build Number: ${env.BUILD_NUMBER}
                            Check details: ${env.BUILD_URL}
                         """
                }
            }
        }

        stage('Build') {
            when {
                not { branch 'main' }
            }
            steps {
                sh '''
                    echo "Building branch: $BRANCH_NAME"
                    echo "Workspace: $WORKSPACE"
                    cd $WORKSPACE
                    mvn package -DskipTests
                '''
            }
        }

//        stage('SonarQube Analysis') {
//            when {
//                not { branch 'main' }
//            }
//            environment {
//                SONAR_TOKEN = credentials('sonarqube_token')
//            }
//            steps {
//                sh '''
//                    mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
//                      -DskipTests \
//                      -Dsonar.projectKey=Jenkins_Demo \
//                      -Dsonar.projectName=Jenkins_Demo \
//                      -Dsonar.host.url=http://65.0.118.121:9000 \
//                      -Dsonar.token=$SONAR_TOKEN
//                '''
//            }
//        }

        // ─────────────────────────────────────────────
        // STAGING ONLY — Save tested JAR to protected folder
        // This is the handoff point between staging → main
        // ─────────────────────────────────────────────

        stage('Save JAR to Protected Folder') {
            when {
                branch 'staging'
            }
            steps {
                sh '''
                    JAR_FILE=$(find $WORKSPACE/target -name "*.jar" ! -name "*sources*" | head -1)

                    if [ -z "$JAR_FILE" ]; then
                        echo "ERROR: No JAR found in $WORKSPACE/target — cannot save to protected folder"
                        exit 1
                    fi

                    # Ensure the protected folder exists
                    mkdir -p $PROTECTED_JAR_DIR

                    # Clear old JARs so main always picks the latest one
                    rm -f $PROTECTED_JAR_DIR/*.jar

                    echo "Saving JAR to protected folder: $PROTECTED_JAR_DIR"
                    cp "$JAR_FILE" "$PROTECTED_JAR_DIR/"

                    echo "JAR saved successfully:"
                    ls -lh $PROTECTED_JAR_DIR/
                '''
            }
        }

        // ─────────────────────────────────────────────
        // APPROVAL — staging needs staging_user
        //            main needs main_user
        //            develop skips this entirely
        // ─────────────────────────────────────────────

        stage('Approval') {
            when {
                anyOf {
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                script {
                    def approver   = ''
                    def targetEnv  = ''

                    if (env.BRANCH_NAME == 'staging') {
                        approver  = 'staging_user'
                        targetEnv = 'STAGING (port 8083)'
                    } else if (env.BRANCH_NAME == 'main') {
                        approver  = 'main_user'
                        targetEnv = 'PRODUCTION (port 8081)'
                    }

                    input(
                        message: "Deploy to ${targetEnv}? This requires approval from ${approver}.",
                        submitter: approver,
                        ok: 'Approve & Deploy'
                    )
                }
            }
        }

        // ─────────────────────────────────────────────
        // DEPLOY — all three branches land here
        // main picks JAR from protected folder
        // develop + staging pick JAR from workspace
        // ─────────────────────────────────────────────

        stage('Deploy') {
            steps {
                sh '''
                    # Resolve port based on branch
                    if [ "$BRANCH_NAME" = "main" ]; then
                        PORT=8081
                    elif [ "$BRANCH_NAME" = "staging" ]; then
                        PORT=8083
                    elif [ "$BRANCH_NAME" = "develop" ]; then
                        PORT=8082
                    else
                        echo "ERROR: Unknown branch '$BRANCH_NAME' — no port mapped"
                        exit 1
                    fi

                    echo "Selected port $PORT for branch $BRANCH_NAME"

                    # ── Resolve JAR location ──────────────────────────────
                    if [ "$BRANCH_NAME" = "main" ]; then
                        # main always uses the pre-tested JAR from the protected folder
                        JAR_FILE=$(find $PROTECTED_JAR_DIR -name "*.jar" ! -name "*sources*" | head -1)
                        echo "main branch — picking JAR from protected folder: $PROTECTED_JAR_DIR"
                    else
                        # develop and staging use the JAR built in this pipeline run
                        JAR_FILE=$(find $WORKSPACE/target -name "*.jar" ! -name "*sources*" | head -1)
                        echo "Branch $BRANCH_NAME — picking JAR from workspace: $WORKSPACE/target"
                    fi

                    if [ -z "$JAR_FILE" ]; then
                        echo "ERROR: No JAR file found — cannot deploy"
                        exit 1
                    fi

                    echo "JAR to deploy: $JAR_FILE"

                    # ── Kill existing process on the port ─────────────────
                    PID=$(lsof -t -i:$PORT || true)
                    if [ -n "$PID" ]; then
                        echo "Killing existing process $PID on port $PORT"
                        kill -9 $PID
                        sleep 3
                    fi

                    # ── Start the application ─────────────────────────────
                    export JENKINS_NODE_COOKIE=dontKillMe
                    nohup java -jar $JAR_FILE --server.port=$PORT > $WORKSPACE/app.log 2>&1 &
                    echo "App started with PID $!"
                    echo "Logs: $WORKSPACE/app.log"
                '''
            }
        }
    }

    // ─────────────────────────────────────────────────────
    // Global post — useful for audit trail
    // ─────────────────────────────────────────────────────
    post {
        success {
            echo "Pipeline completed successfully for branch: ${env.BRANCH_NAME}"
        }
        failure {
            echo "Pipeline FAILED for branch: ${env.BRANCH_NAME}"
        }
        aborted {
            echo "Pipeline was ABORTED (approval likely rejected) for branch: ${env.BRANCH_NAME}"
        }
    }
}