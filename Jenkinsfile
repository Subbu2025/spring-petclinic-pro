@Library('petclinic-shared-library') _

pipeline {
    agent any

    environment {
        KUBERNETES_NAMESPACE = '' 
        TARGET_ENV = '' 
        HELM_RELEASE_NAME = '' // Dynamically assigned
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
                        HELM_RELEASE_NAME = "mysql-${TARGET_ENV}"
                    } else if (env.BRANCH_NAME == 'main') {
                        def userInput = input message: "Deploy to which environment?", parameters: [
                            choice(choices: ['uat', 'prod'], description: 'Choose the deployment environment', name: 'DEPLOY_ENV')
                        ]

                        if (userInput == 'uat') {
                            KUBERNETES_NAMESPACE = 'petclinic-uat'
                            TARGET_ENV = 'uat'
                            HELM_RELEASE_NAME = "mysql-${TARGET_ENV}"
                        } else if (userInput == 'prod') {
                            // Notify stakeholders for production approval
                            mail to: 'ssrmca07@gmail.com',
                                subject: "Production Deployment Approval Required",
                                body: """
                                A request to deploy to production has been made. Please confirm before proceeding.
                                Pipeline: ${env.JOB_NAME}
                                Build Number: ${env.BUILD_NUMBER}
                                Link to approve or reject: ${env.BUILD_URL}
                                """

                            // Restrict PROD approval to authorized personnel
                            def approver = input message: "Only authorized personnel can approve PROD deployments. Enter your username to confirm.", parameters: [
                                string(name: 'APPROVER', description: 'Enter your username')
                            ]

                            if (approver != 'admin') {
                                error "Unauthorized user '${approver}' attempted to approve a production deployment."
                            }

                            // Final confirmation with timeout
                            timeout(time: 10, unit: 'MINUTES') {
                                input message: "Are you sure you want to deploy to PROD? This action is irreversible.", ok: "Yes, Deploy to PROD"
                            }

                            KUBERNETES_NAMESPACE = 'petclinic-prod'
                            TARGET_ENV = 'prod'
                            HELM_RELEASE_NAME = "mysql-${TARGET_ENV}"
                        } else {
                            error "Invalid deployment environment selected."
                        }
                    } else {
                        error "Unknown branch: ${env.BRANCH_NAME}"
                    }

                    echo "Target environment: ${TARGET_ENV}"
                    echo "Target namespace: ${KUBERNETES_NAMESPACE}"
                    echo "Helm Release Name: ${HELM_RELEASE_NAME}"
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
                        try {
                            sh """
                            aws sts get-caller-identity
                            aws eks update-kubeconfig \
                                --region ap-south-1 \
                                --name devops-petclinicapp-dev-ap-south-1 \
                                --alias devops-petclinicapp
                            echo "Kubeconfig Path: ${KUBECONFIG_PATH}"
                            cat ${KUBECONFIG_PATH}
                            kubectl get nodes --kubeconfig ${KUBECONFIG_PATH}
                            """
                        } catch (Exception e) {
                            error "Failed to set up Kubernetes access. Check AWS credentials, EKS cluster, or kubeconfig path."
                        }
                    }
                }
            }
        }


        stage('Fetch Helm Charts') {
            steps {
                script {
                    dir('helm_charts') {
                        echo "Cloning Helm charts repository..."
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: HELM_CHART_REPO_BRANCH]],
                            userRemoteConfigs: [[
                                url: HELM_CHART_REPO_URL,
                                credentialsId: 'Subbu2025_github-creds'
                            ]]
                        ])

                        echo "Listing workspace structure after cloning Helm charts:"
                        sh "ls -Rla"

                        echo "Validating Helm chart structure..."
                        if (!fileExists('charts/mysql-chart')) {
                            error "MySQL chart directory not found under 'charts/'. Check repository structure."
                        }

                        echo "Adding Bitnami Helm repository..."
                        sh "helm repo add bitnami https://charts.bitnami.com/bitnami"

                        echo "Updating Helm repositories..."
                        sh "helm repo update"

                        echo "Building dependencies for Helm charts..."
                        sh "helm dependency build charts/mysql-chart"
                    }

                    stash name: 'helm-charts', includes: 'helm_charts/**'
                }
            }
        }

            stage('Apply ConfigMaps and Secrets') {
                steps {
                    script {
                        withCredentials([[ 
                            $class: 'AmazonWebServicesCredentialsBinding', 
                            credentialsId: 'aws-eks-credentials' 
                        ]]) {
                            echo "Unstashing Helm charts..."
                            unstash 'helm-charts'
            
                            echo "Building Helm dependencies..."
                            sh """
                            helm dependency build helm_charts/charts/petclinic-chart
                            """
            
                            echo "Checking and applying ConfigMaps and Secrets..."
            
                            def configMapExists = sh(
                                script: "kubectl get configmap app-config-${TARGET_ENV} -n ${KUBERNETES_NAMESPACE} || echo 'not_found'",
                                returnStdout: true
                            ).trim()
            
                            if (configMapExists.contains('not_found')) {
                                echo "ConfigMap not found. Rendering and applying ConfigMap..."
                                sh """
                                helm template helm_charts/charts/petclinic-chart \
                                    --set environment.name=${TARGET_ENV} \
                                    --set environment.namespace=${KUBERNETES_NAMESPACE} \
                                    | kubectl apply -f -
                                """
                            } else {
                                echo "ConfigMap already exists."
                            }
            
                            def secretExists = sh(
                                script: "kubectl get secret ${TARGET_ENV}-secrets -n ${KUBERNETES_NAMESPACE} || echo 'not_found'",
                                returnStdout: true
                            ).trim()
            
                            if (secretExists.contains('not_found')) {
                                echo "Secret not found. Rendering and applying ExternalSecret..."
                                sh """
                                helm template helm_charts/charts/petclinic-chart \
                                    --set environment.name=${TARGET_ENV} \
                                    --set environment.namespace=${KUBERNETES_NAMESPACE} \
                                    | kubectl apply -f -
                                """
                            } else {
                                echo "Secret already exists."
                            }
                        }
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

        stage('Deploy MySQL Chart') {
            steps {
                unstash 'helm-charts'
                retry(3) {
                    script {
                        withCredentials([[ 
                            $class: 'AmazonWebServicesCredentialsBinding',
                            credentialsId: 'aws-eks-credentials'
                        ]]) {
                            sh """
                            echo "Listing contents of Helm charts directory:"
                            ls -la helm_charts/charts
                            helm upgrade --install ${HELM_RELEASE_NAME} helm_charts/charts/mysql-chart \
                              -f helm_charts/charts/mysql-chart/environments/${TARGET_ENV}/mysql-values.yaml \
                              --set serviceAccount.name=secrets-manager-sa \
                              -n ${KUBERNETES_NAMESPACE} \
                              --kubeconfig ${KUBECONFIG_PATH} --debug
                            """
                        }
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

        stage('Deploy PetClinic Chart') {
            steps {
                unstash 'helm-charts'
                script {
                    withCredentials([[ 
                        $class: 'AmazonWebServicesCredentialsBinding', 
                        credentialsId: 'aws-eks-credentials' 
                    ]]) {
                        sh """
                            helm upgrade --install petclinic helm_charts/charts/petclinic-chart \
                              -f helm_charts/charts/petclinic-chart/environments/${TARGET_ENV}/petclinic-values.yaml \
                              --set app.image.tag=${BUILD_TAG} \
                              --set app.image.repository=905418425077.dkr.ecr.ap-south-1.amazonaws.com/dev-qa/petclinic \
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
        always {
            echo "Cleaning up workspace to avoid conflicts in subsequent runs..."
            deleteDir()
        }
    }
}
