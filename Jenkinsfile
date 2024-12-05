@Library('petclinic-shared-library') _

pipeline {
    agent any

    environment {
        KUBERNETES_NAMESPACE = 'petclinic-dev-qa' // Namespace dynamically set
        TARGET_ENV = 'dev-qa'           // Environment (dev-qa, uat, or prod)
        HELM_RELEASE_NAME = ''    // Helm release name dynamically set
    }

    stages {
        stage('Setup Environment') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'develop') {
                        KUBERNETES_NAMESPACE = 'petclinic-dev-qa'
                        TARGET_ENV = 'dev-qa'
                        HELM_RELEASE_NAME = 'petclinic-dev-qa'
                    } else if (env.BRANCH_NAME == 'main') {
                        // Prompt user for environment selection
                        def userInput = input message: "Deploy to which environment?", parameters: [
                            choice(choices: ['uat', 'prod'], description: 'Choose the deployment environment', name: 'DEPLOY_ENV')
                        ]

                        if (userInput == 'uat') {
                            KUBERNETES_NAMESPACE = 'petclinic-uat'
                            TARGET_ENV = 'uat'
                            HELM_RELEASE_NAME = 'petclinic-uat'
                        } else if (userInput == 'prod') {
                            // Notify stakeholders for production deployment
                            mail to: 'ssrmca07@gmail.com',
                                subject: "Production Deployment Approval Required",
                                body: """
                                A request to deploy to production has been made.
                                Pipeline: ${env.JOB_NAME}
                                Build Number: ${env.BUILD_NUMBER}
                                Link: ${env.BUILD_URL}
                                """

                            // Restrict production approval to authorized personnel
                            def approver = input message: "Only authorized personnel can approve PROD deployments. Enter your username:", parameters: [
                                string(name: 'APPROVER', description: 'Enter your username')
                            ]

                            if (approver != 'admin') {
                                error "Unauthorized user attempted to approve a production deployment."
                            }

                            // Final confirmation before deploying to production
                            timeout(time: 10, unit: 'MINUTES') {
                                input message: "Confirm deployment to PROD. This action is irreversible.", ok: "Yes, Deploy"
                            }

                            KUBERNETES_NAMESPACE = 'petclinic-prod'
                            TARGET_ENV = 'prod'
                            HELM_RELEASE_NAME = 'petclinic-prod'
                        } else {
                            error "Invalid deployment environment selected."
                        }
                    } else {
                        error "Unsupported branch: ${env.BRANCH_NAME}"
                    }

                    echo "Target Environment: ${TARGET_ENV}"
                    echo "Kubernetes Namespace: ${KUBERNETES_NAMESPACE}"
                }
            }
        }

        stage('Setup Kubernetes Access') {
            steps {
                script {
                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: 'aws-eks-credentials'
                    ]]) {
                        sh """
                        aws eks update-kubeconfig --region ap-south-1 --name devops-petclinicapp-dev-ap-south-1-${TARGET_ENV}
                        """
                    }
                }
            }
        }

        stage('Checkout Code') {
            steps {
                script {
                    checkoutCode(
                        url: 'https://github.com/Subbu2025/spring-petclinic-pro.git',
                        credentialsId: 'github-credentials',
                        branch: env.BRANCH_NAME
                    )
                }
            }
        }

        stage('Deploy MySQL') {
            steps {
                script {
                    sh """
                    helm upgrade --install mysql-${TARGET_ENV} ./charts/mysql-chart \
                      -f ./charts/mysql-chart/environments/${TARGET_ENV}/mysql-values.yaml \
                      -n ${KUBERNETES_NAMESPACE}
                    """
                }
            }
        }

        stage('Deploy PetClinic') {
            steps {
                script {
                    sh """
                    helm upgrade --install ${HELM_RELEASE_NAME} ./charts/petclinic-chart \
                      -f ./charts/petclinic-chart/environments/${TARGET_ENV}/petclinic-values.yaml \
                      -n ${KUBERNETES_NAMESPACE}
                    """
                }
            }
        }

        stage('Run Helm Tests') {
            when {
                expression { TARGET_ENV != 'prod' } // Skip Helm tests for production
            }
            steps {
                script {
                    sh """
                    helm test ${HELM_RELEASE_NAME} -n ${KUBERNETES_NAMESPACE}
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Deployment completed successfully for ${TARGET_ENV}."
        }
        failure {
            echo "Deployment failed for ${TARGET_ENV}."
        }
    }
}
