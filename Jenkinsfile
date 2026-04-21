pipeline {
    agent { label 'ec2-agent' }

    options {
        skipDefaultCheckout(true)
    }
    stages {

         stage('Checkout') {
                steps {
                     git credentialsId: 'github-classic-token',
                         url: 'https://github.com/Rishabhgoyal0183/testApplication.git',
                         branch: 'master'
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
                     junit '**/target/surefire-reports/*.xml'
                 }
             }
         }


        stage('SonarQube Analysis') {

            steps {
                withSonarQubeEnv('sonarqube-server') {
                    sh """
                    mvn clean verify sonar:sonar \
                      -DskipTests \
                      -Dsonar.projectKey=testApplication \
                      -Dsonar.projectName=testApplication \
                      -Dsonar.host.url=$SONAR_HOST_URL \
                      -Dsonar.token=$SONAR_AUTH_TOKEN
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

        stage('Deploy') {
            steps {
                sh '''
                    if [ "$BRANCH_NAME" = "master" ]; then
                        PORT=8081
                    elif [ "$BRANCH_NAME" = "Dev" ]; then
                        PORT=8082
                    elif [ "$BRANCH_NAME" = "UAT" ]; then
                        PORT=8083
                    fi

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
                    nohup java -javaagent:/opt/jmx_prometheus_javaagent-1.1.0.jar=8080:/opt/jmx-config.yaml -jar $JAR_FILE --server.port=$PORT > $WORKSPACE/app.log 2>&1 &

                    echo "App started with PID $!"
                    echo "Logs: $WORKSPACE/app.log"
                '''
            }
        }
    }
}