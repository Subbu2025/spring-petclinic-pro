@Library('petclinic-shared-library') _

pipeline {
    agent any

    environment {
        KUBERNETES_NAMESPACE = '' // Namespace (dynamically set based on branch)
        TARGET_ENV = ''           // Deployment environment (dev-qa, uat, or prod)
        ECR_URL = '905418425077.dkr.ecr.ap-south-1.amazonaws.com' // AWS ECR URL
        IMAGE_NAME = 'spring-petclinic' // Docker image name
        GITHUB_CREDENTIALS_ID = 'Subbu2025_github-creds' // GitHub credentials ID
        AWS_CREDENTIALS_ID = 'aws-eks-credentials'       // AWS credentials ID
        GITHUB_REPO_URL = 'https://github.com/Subbu2025/spring-petclinic-pro.git' // GitHub repo URL
    }

    stages {
        stage('Setup') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'develop') {
                        KUBERNETES_NAMESPACE = 'petclinic-dev-qa'
                        TARGET_ENV = 'dev-qa'
                    } else if (env.BRANCH_NAME == 'main') {
                        def userInput = input message: "Deploy to which environment?", parameters: [
                            choice(choices: ['uat', 'prod'], description: 'Choose the deployment environment', name: 'DEPLOY_ENV')
                        ]

                        if (userInput == 'uat') {
                            KUBERNETES_NAMESPACE = 'petclinic-uat'
                            TARGET_ENV = 'uat'
                        } else if (userInput == 'prod') {
                            // Email stakeholders
                            mail to: 'ssrmca07@gmail.com',
                                subject: "Production Deployment Approval Required",
                                body: """
                                A request to deploy to production has been made. Please confirm before proceeding.
                                Pipeline: ${env.JOB_NAME}
                                Build Number: ${env.BUILD_NUMBER}
                                Link to approve or reject: ${env.BUILD_URL}
                                """

                            // Restrict prod approval to authorized personnel
                            def approver = input message: "Only authorized personnel can approve PROD deployments. Enter your username to confirm.", parameters: [
                                string(name: 'APPROVER', description: 'Enter your username')
                            ]

                            if (approver != 'admin') {
                                error "Unauthorized user attempted to approve a production deployment."
                            }

                            // Final confirmation
                            timeout(time: 10, unit: 'MINUTES') {
                                input message: "Are you sure you want to deploy to PROD? This action is irreversible.", ok: "Yes, Deploy to PROD"
                            }

                            KUBERNETES_NAMESPACE = 'petclinic-prod'
                            TARGET_ENV = 'prod'
                        } else {
                            error "Invalid deployment environment selected."
                        }
                    } else {
                        error "Unknown branch: ${env.BRANCH_NAME}"
                    }

                    echo "Target environment: ${TARGET_ENV}"
                    echo "Target namespace: ${KUBERNETES_NAMESPACE}"
                }
            }
        }

        stage('Clone Helm Charts Repository') {
            steps {
                script {
                    git(
                        url: 'https://github.com/Subbu2025/PetClinic-Helm-Charts.git',
                        credentialsId: GITHUB_CREDENTIALS_ID,
                        branch: 'main'
                    )
                }
            }
        }

        stage('Checkout Application Code') {
            steps {
                script {
                    checkoutCode(
                        url: GITHUB_REPO_URL,
                        credentialsId: GITHUB_CREDENTIALS_ID,
                        branch: env.BRANCH_NAME
                    )
                }
            }
        }

        stage('Unit Testing') {
            steps {
                script {
                    unitTest(
                        testCommand: './mvnw clean test -Dsurefire.reportFormat=xml',
                        stageName: 'Unit Tests',
                        reportDir: 'target/surefire-reports'
                    )
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    sonarQubeAnalysis(
                        namespace: KUBERNETES_NAMESPACE,
                        awsCredentialsId: AWS_CREDENTIALS_ID
                    )
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dockerBuildAndPushScript(
                        imageName: IMAGE_NAME,
                        awsCredentialsId: AWS_CREDENTIALS_ID,
                        ecrUrl: ECR_URL
                    )
                }
            }
        }

        stage('Deploy Using Helm Charts') {
            steps {
                script {
                    dir('charts/petclinic-chart') { // Navigate to the cloned Helm chart directory
                        sh """
                            helm upgrade --install petclinic-app . \
                              -n ${KUBERNETES_NAMESPACE} --create-namespace \
                              --set namespace=${KUBERNETES_NAMESPACE},image.repository=${ECR_URL}/${IMAGE_NAME},image.tag=latest
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully for branch: ${env.BRANCH_NAME}, environment: ${TARGET_ENV}"
        }
        failure {
            echo "Pipeline failed for branch: ${env.BRANCH_NAME}"
        }
    }
}
