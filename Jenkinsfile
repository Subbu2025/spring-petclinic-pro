@Library('petclinic-shared-library') _

pipeline {
    agent any

    environment {
        KUBERNETES_NAMESPACE = '' 
        TARGET_ENV = ''          
        HELM_RELEASE_NAME = ''   
        HELM_CHART_REPO_URL = 'https://github.com/Subbu2025/PetClinic-Helm-Charts.git'
        HELM_CHART_REPO_BRANCH = 'main'
        KUBECONFIG_PATH = '/var/lib/jenkins/.kube/config'
    }

    stages {
        stage('Setup Environment') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'develop') {
                        KUBERNETES_NAMESPACE = 'petclinic-dev-qa'
                        TARGET_ENV = 'dev-qa'
                        HELM_RELEASE_NAME = 'mysql-dev-qa'
                    } else if (env.BRANCH_NAME == 'main') {
                        def userInput = input message: "Deploy to which environment?", parameters: [
                            choice(choices: ['uat', 'prod'], description: 'Choose the deployment environment', name: 'DEPLOY_ENV')
                        ]
                        if (userInput == 'uat') {
                            KUBERNETES_NAMESPACE = 'petclinic-uat'
                            TARGET_ENV = 'uat'
                            HELM_RELEASE_NAME = 'mysql-uat'
                        } else if (userInput == 'prod') {
                            KUBERNETES_NAMESPACE = 'petclinic-prod'
                            TARGET_ENV = 'prod'
                            HELM_RELEASE_NAME = 'mysql-prod'
                        } else {
                            error "Invalid deployment environment selected."
                        }
                    } else {
                        error "Unsupported branch: ${env.BRANCH_NAME}."
                    }
                    echo "Target Environment: ${TARGET_ENV}"
                    echo "Kubernetes Namespace: ${KUBERNETES_NAMESPACE}"
                }
            }
        }

        stage('Fetch Helm Charts') {
            steps {
                script {
                    echo "Cloning Helm charts repository..."
                    checkout([ 
                        $class: 'GitSCM',
                        branches: [[name: HELM_CHART_REPO_BRANCH]],
                        userRemoteConfigs: [[
                            url: HELM_CHART_REPO_URL,
                            credentialsId: 'Subbu2025_github-creds'
                        ]]
                    ])
                    if (!fileExists('charts')) {
                        error "Charts directory not found! Ensure Helm chart repo is correctly cloned."
                    }
                    echo "Building Helm chart dependencies..."
                    sh """
                    helm dependency update ./charts/mysql-chart
                    helm dependency update ./charts/petclinic-chart
                    """
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
                        aws sts get-caller-identity
                        aws eks update-kubeconfig \
                            --region ap-south-1 \
                            --name devops-petclinicapp-dev-ap-south-1 \
                            --alias devops-petclinicapp
                        kubectl get nodes --kubeconfig ${KUBECONFIG_PATH}
                        """
                    }
                }
            }
        }

        stage('Deploy MySQL Chart') {
            steps {
                retry(3) {
                    script {
                        withCredentials([[ 
                            $class: 'AmazonWebServicesCredentialsBinding', 
                            credentialsId: 'aws-eks-credentials' 
                        ]]) {
                            sh """
                            AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
                            AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
                            helm upgrade --install ${HELM_RELEASE_NAME} ./charts/mysql-chart \
                              -f ./charts/mysql-chart/environments/${TARGET_ENV}/mysql-values.yaml \
                              --set serviceAccount.name=secrets-manager-sa \
                              -n ${KUBERNETES_NAMESPACE} \
                              --kubeconfig ${KUBECONFIG_PATH} --debug
                            """
                        }
                    }
                }
            }
        }

        stage('Health Check: MySQL') {
            steps {
                script {
                    echo "Checking readiness for MySQL..."
                    sh """
                    kubectl rollout status deployment/${HELM_RELEASE_NAME} -n ${KUBERNETES_NAMESPACE} --timeout=120s
                    """
                }
            }
        }

        stage('Deploy PetClinic Chart') {
            steps {
                script {
                    withCredentials([[ 
                        $class: 'AmazonWebServicesCredentialsBinding', 
                        credentialsId: 'aws-eks-credentials' 
                    ]]) {
                        sh """
                        AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
                        AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
                        helm upgrade --install petclinic ./charts/petclinic-chart \
                          -f ./charts/petclinic-chart/environments/${TARGET_ENV}/petclinic-values.yaml \
                          --set serviceAccount.name=secrets-manager-sa \
                          -n ${KUBERNETES_NAMESPACE} \
                          --kubeconfig ${KUBECONFIG_PATH} --debug
                        """
                    }
                }
            }
        }

        stage('Health Check: PetClinic') {
            steps {
                script {
                    echo "Checking readiness for PetClinic..."
                    sh """
                    kubectl rollout status deployment/petclinic -n ${KUBERNETES_NAMESPACE} --timeout=180s
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
            echo "Deployment failed for ${TARGET_ENV}. Collecting logs for debugging..."
            sh """
            kubectl get all -n ${KUBERNETES_NAMESPACE} || true
            kubectl logs -l app=mysql -n ${KUBERNETES_NAMESPACE} || true
            kubectl logs -l app=petclinic -n ${KUBERNETES_NAMESPACE} || true
            """
        }
    }
}
