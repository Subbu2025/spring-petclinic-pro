@Library('petclinic-shared-library') _

pipeline {
    agent any

    environment {
        KUBERNETES_NAMESPACE = '' // Namespace dynamically set
        TARGET_ENV = ''           // Environment (dev-qa, uat, or prod)
        HELM_RELEASE_NAME = ''    // Release name dynamically set
        HELM_CHART_REPO_URL = 'https://github.com/Subbu2025/PetClinic-Helm-Charts.git'
        HELM_CHART_REPO_BRANCH = 'main'
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
                        def userInput = input message: "Deploy to which environment?", parameters: [
                            choice(choices: ['uat', 'prod'], description: 'Choose the deployment environment', name: 'DEPLOY_ENV')
                        ]

                        if (userInput == 'uat') {
                            KUBERNETES_NAMESPACE = 'petclinic-uat'
                            TARGET_ENV = 'uat'
                            HELM_RELEASE_NAME = 'petclinic-uat'
                        } else if (userInput == 'prod') {
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
                        echo "Validating AWS credentials and updating kubeconfig..."
                        sh """
                        echo "AWS_ACCESS_KEY_ID: $AWS_ACCESS_KEY_ID"
                        echo "AWS_SECRET_ACCESS_KEY: $AWS_SECRET_ACCESS_KEY"
                        
                        # Validate AWS Credentials
                        aws sts get-caller-identity
        
                        # Update kubeconfig for the EKS cluster
                        aws eks update-kubeconfig \
                            --region ap-south-1 \
                            --name devops-petclinicapp-dev-ap-south-1
                        
                        # Validate Kubernetes cluster access
                        kubectl get nodes --kubeconfig /var/lib/jenkins/.kube/config
                        """
                    }
                }
            }
        }


        stage('Load ConfigMaps and Secrets') {
            steps {
                script {
                    echo "Loading ConfigMaps and Secrets for ${TARGET_ENV} using Helm..."
                    sh """
                    # Deploy ConfigMap and Secrets via Helm for MySQL
                    helm upgrade --install mysql-${TARGET_ENV} ./charts/mysql-chart \
                      -f ./charts/mysql-chart/environments/${TARGET_ENV}/mysql-values.yaml \
                      -n ${KUBERNETES_NAMESPACE} \
                      --kubeconfig /var/lib/jenkins/.kube/config
        
                    # Deploy ConfigMap and Secrets via Helm for PetClinic
                    helm upgrade --install ${HELM_RELEASE_NAME} ./charts/petclinic-chart \
                      -f ./charts/petclinic-chart/environments/${TARGET_ENV}/petclinic-values.yaml \
                      -n ${KUBERNETES_NAMESPACE} \
                      --kubeconfig /var/lib/jenkins/.kube/config
                    """
                }
            }
        }


        stage('Deploy MySQL') {
            steps {
                script {
                    withCredentials([[ 
                        $class: 'AmazonWebServicesCredentialsBinding', 
                        credentialsId: 'aws-eks-credentials'
                    ]]) {
                        sh """
                        AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
                        AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
                        helm upgrade --install mysql-${TARGET_ENV} ./charts/mysql-chart \
                          -f ./charts/mysql-chart/environments/${TARGET_ENV}/mysql-values.yaml \
                          -n ${KUBERNETES_NAMESPACE} \
                          --kubeconfig /var/lib/jenkins/.kube/config
                        """
                    }
                }
            }
        }

        stage('Deploy PetClinic') {
            steps {
                script {
                    withCredentials([[ 
                        $class: 'AmazonWebServicesCredentialsBinding', 
                        credentialsId: 'aws-eks-credentials'
                    ]]) {
                        sh """
                        AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
                        AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
                        helm upgrade --install ${HELM_RELEASE_NAME} ./charts/petclinic-chart \
                          -f ./charts/petclinic-chart/environments/${TARGET_ENV}/petclinic-values.yaml \
                          -n ${KUBERNETES_NAMESPACE} \
                          --kubeconfig /var/lib/jenkins/.kube/config
                        """
                    }
                }
            }
        }

        stage('Run Helm Tests') {
            when {
                expression { TARGET_ENV != 'prod' }
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
