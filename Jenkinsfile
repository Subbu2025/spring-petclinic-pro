@Library('petclinic-shared-library') _

pipeline {
    agent any

    environment {
        KUBERNETES_NAMESPACE = '' // Namespace (will be dynamically set)
        TARGET_ENV = ''           // Deployment environment (dev-qa, uat, or prod)
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

        stage('Checkout Code') {
            steps {
                script {
                    checkoutCode(
                        url: 'https://github.com/Subbu2025/spring-petclinic-pro.git',
                        credentialsId: 'Subbu2025_github-creds',
                        branch: env.BRANCH_NAME
                    )
                }
            }
        }

        stage('Unit Testing') {
            steps {
                script {
                    runTests(
                        testCommand: './mvnw test',
                        stageName: 'Unit Tests',
                        reportDir: 'target/surefire-reports'
                    )
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
