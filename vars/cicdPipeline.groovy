def call(Map config = [:]) {
    pipeline {
        agent { label config.agent ?: 'jenkins-agent' }

        stages {
            stage('Checkout') {
                steps {
                    git url: config.repo, branch: config.branch
                }
            }

            stage('Build') {
                steps {
                    sh 'docker compose build'
                }
            }

            stage('Test') {
                steps {
                    sh 'docker compose run --rm notes-app python manage.py test || true'
                }
            }

            stage('Push to DockerHub') {
                steps {
                    withCredentials([usernamePassword(credentialsId: config.dockerCreds,
                                                      usernameVariable: 'DOCKERHUB_USER',
                                                      passwordVariable: 'DOCKERHUB_PASS')]) {
                        sh """
                            echo $DOCKERHUB_PASS | docker login -u $DOCKERHUB_USER --password-stdin
                            docker tag notes-app:latest ${config.dockerRepo}:${env.BUILD_NUMBER}
                            docker tag notes-app:latest ${config.dockerRepo}:latest
                            docker push ${config.dockerRepo}:${env.BUILD_NUMBER}
                            docker push ${config.dockerRepo}:latest
                        """
                    }
                }
            }

            stage('Deploy') {
                steps {
                    sh '''
                        docker compose down || true
                        docker compose pull
                        docker compose up -d
                    '''
                }
            }
        }
    }
}
