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

         stage('Setup Kubernetes Access') {
            steps {
                script {
                    withCredentials([[ 
                        $class: 'AmazonWebServicesCredentialsBinding', 
                        credentialsId: 'aws-eks-credentials' 
                    ]]) {
                        echo "Updating kubeconfig for EKS cluster access..."
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
            
                        // Add the missing Bitnami Helm repository
                        echo "Adding Bitnami Helm repository..."
                        sh "helm repo add bitnami https://charts.bitnami.com/bitnami"
                        
                        // Update repositories to fetch metadata
                        echo "Updating Helm repositories..."
                        sh "helm repo update"
            
                        echo "Building dependencies for Helm charts..."
                        sh "helm dependency build ./charts/petclinic-chart"
                    }
                }
            }

   
        stage('Validate ConfigMaps and Secrets') {
            steps {
                script {
                    withCredentials([[ 
                        $class: 'AmazonWebServicesCredentialsBinding', 
                        credentialsId: 'aws-eks-credentials'
                    ]]) {
                        echo "Validating ConfigMaps and Secrets in namespace ${KUBERNETES_NAMESPACE}..."
                        sh """
                        kubectl get configmap app-config-${TARGET_ENV} -n ${KUBERNETES_NAMESPACE} || echo 'ConfigMap missing'
                        kubectl get secret ${TARGET_ENV}-secrets -n ${KUBERNETES_NAMESPACE} || echo 'Secret missing'
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
                        credentialsId: 'Subbu2025_github-creds',
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

        stage('Build and Push Docker Image') {
            steps {
                script {
                    buildAndPushDocker(
                        repoUrl: '905418425077.dkr.ecr.ap-south-1.amazonaws.com/dev-qa/petclinic',
                        awsCredentialsId: "aws-eks-credentials",
                        dockerImageName: "spring-petclinic",
                        prId: env.CHANGE_ID, // Use PR ID from environment if available
                        shortCommitSha: env.GIT_COMMIT?.take(7) // Shortened Git SHA
                    )
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

        stage('SonarQube Analysis') {
            steps {
                script {
                    sonarQubeAnalysis(
                        namespace: KUBERNETES_NAMESPACE,
                        awsCredentialsId: "aws-eks-credentials",
                        debug: true
                    )
                }
            }
        }


        stage('Run Helm Tests') {
            when {
                expression { TARGET_ENV != 'prod' }
            }
            steps {
                script {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        sh """
                        helm test ${HELM_RELEASE_NAME} -n ${KUBERNETES_NAMESPACE} --kubeconfig ${KUBECONFIG_PATH}
                        """
                    }
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
            kubectl get all -n ${KUBERNETES_NAMESPACE}
            kubectl logs -l app=petclinic -n ${KUBERNETES_NAMESPACE}
            """
        }
    }
}
