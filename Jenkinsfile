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
                    // Branch-to-Environment Mapping
                    if (env.BRANCH_NAME == 'develop') {
                        KUBERNETES_NAMESPACE = 'petclinic-dev-qa'
                        TARGET_ENV = 'dev-qa'
                    } else if (env.BRANCH_NAME == 'main') {
                        // Ask the user for UAT or Prod deployment
                        input message: "Deploy to which environment?", parameters: [
                            choice(choices: ['uat', 'prod'], description: 'Choose the deployment environment', name: 'DEPLOY_ENV')
                        ]
                        if (params.DEPLOY_ENV == 'uat') {
                            KUBERNETES_NAMESPACE = 'petclinic-uat'
                            TARGET_ENV = 'uat'
                        } else if (params.DEPLOY_ENV == 'prod') {
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
                    // Use the shared library to check out the code
                    checkoutCode(
                        url: 'https://github.com/Subbu2025/spring-petclinic-pro.git',
                        credentialsId: 'Subbu2025_github-creds',
                        branch: env.BRANCH_NAME
                    )
                }
            }
        }
    }

    post {
        success {
            echo "Code checkout completed successfully for branch: ${env.BRANCH_NAME}, environment: ${TARGET_ENV}"
        }
        failure {
            echo "Code checkout failed for branch: ${env.BRANCH_NAME}"
        }
    }
}
