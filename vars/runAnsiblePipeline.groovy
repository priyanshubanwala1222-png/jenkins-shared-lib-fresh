#!/usr/bin/env groovy

def call(Map config = [:]) {
    def slackChannel = config.get('SLACK_CHANNEL_NAME', 'jenkins-notification')
    def environment = config.get('ENVIRONMENT', 'prod')
    def repoURL = config.get('REPO_UR', 'https://github.com/priyanshubanwala1222-png/Batch.34.2026.git')
    def branch = config.get('BRANCH', 'main')
    def basePath = config.get('CODE_BASE_PATH', "env/${environment}")
    def actionMessage = config.get('ACTION_MESSAGE', "Deploying Nginx to ${environment}")
    def skipApproval  = config.get('KEEP_APPROVAL_STAGE', true).toString().toBoolean() == false
    
    pipeline {
        agent {label 'assign-6'}

        stages{
            stage('Clone Repository'){
                steps{
                    echo "Cloning Ansible configuration from ${repoUrl} [${branch}]..."
                    cleanWs()
                    checkout([$class: 'GitSCM',
                        branches :[[name: "*/${branch}"]],
                        userRemoteConfigs: [[url: repoURL]]
                    ])
                }
            }

            stage('User Approval'){
                when {
                    expression { return !skipApproval }
                }
                steps {
                    slackSend(channel: slackChannel, color: '#FFFF00',
                        message: "PAUSED: ${actionMessage} awaiting manual approval in Jenkins: ${env.BUILD_URL}"
                    )
                    input message: "Approve deployment of Nginx to ${environment}?", ok: "Proceed Deployment"
                }
            }

            stage('Playbook Execution') {
                steps{
                    echo "Executing Ansible Playbook from path: ${basePath}"

                    dir("${basePath}") {
                        sh "ansible-playbook -i inventory site.yml"
                    }
                }
            }
        }

        post{
            success{
                slackSend( channel: slackChannel, color: '#00FF00',
                    message: "SUCCESS: ${actionMessage} complete successfully. Build #${env.BUILD_NUMBER} (${env.BUILD_URL})"
                )
            }
            failure {
                slackSend(channel: slackChannel, color: "#ff0000",
                    message: "FAILURE: ${actionmessage} failed during execution. Check logs: ${env.BUILD_URL}"
                )
            }
        }
    }
}
