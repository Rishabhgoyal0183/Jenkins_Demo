pipeline {
    agent any

options {
        skipDefaultCheckout(true)
    }

    stages {

         stage('Checkout') {
                     steps {
                         checkout scm
                     }
          }

         stage('Compile') {
             steps {
                 sh 'mvn clean compile'
             }
         }

        stage('Run Unit Tests') {
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
            steps {
                sh '''
                    echo "Building branch: $BRANCH_NAME"
                    echo "Workspace: $WORKSPACE"
                    cd $WORKSPACE
                    mvn package -DskipTests
                '''
            }
        }

        stage('SonarQube Analysis') {
            environment {
                SONAR_TOKEN = credentials('sonarqube_token')
            }
            steps {
                sh '''
                    mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                      -Dsonar.projectKey=Jenkins_Demo \
                      -Dsonar.projectName=Jenkins_Demo \
                      -Dsonar.host.url=http://65.0.118.121:9000 \
                      -Dsonar.token=$SONAR_TOKEN
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    if [ "$BRANCH_NAME" = "main" ]; then
                        PORT=8081
                    elif [ "$BRANCH_NAME" = "develop" ]; then
                        PORT=8082
                    fi
                    echo "Selected port $PORT for branch $BRANCH_NAME"

                    # Kill existing process on the port
                    PID=$(lsof -t -i:$PORT || true)
                    if [ -n "$PID" ]; then
                        echo "Killing existing process $PID on port $PORT"
                        kill -9 $PID
                        sleep 3
                    fi

                    # Find JAR from the correct branch workspace
                    JAR_FILE=$(find $WORKSPACE/target -name "*.jar" ! -name "*sources*" | head -1)

                    if [ -z "$JAR_FILE" ]; then
                        echo "ERROR: No JAR file found in $WORKSPACE/target"
                        exit 1
                    fi

                    echo "Deploying branch '$BRANCH_NAME' on port $PORT"
                    echo "JAR: $JAR_FILE"

                    export JENKINS_NODE_COOKIE=dontKillMe
//                    nohup java -javaagent:/opt/jmx_prometheus_javaagent-1.1.0.jar=8080:/opt/jmx-config.yaml -jar $JAR_FILE --server.port=$PORT > $WORKSPACE/app.log 2>&1 &
                    nohup java -jar $JAR_FILE --server.port=$PORT > $WORKSPACE/app.log 2>&1 &
                    echo "App started with PID $!"
                    echo "Logs: $WORKSPACE/app.log"
                '''
            }
        }
    }
}