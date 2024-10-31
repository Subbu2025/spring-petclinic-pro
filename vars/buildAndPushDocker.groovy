// vars/buildAndPushDocker.groovy

def call(Map params) {
    // Ensure required parameters are passed
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('prId') : "Missing required parameter: prId"

    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String prId = params.prId
    String buildNumber = env.BUILD_NUMBER
    String fullImageTag = "${dockerImageName}:${prId}-${buildNumber}"

    stage("Build Docker Image") {
        sh "docker build -t ${fullImageTag} ."
    }

    stage("Tag and Push Docker Image to ECR") {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
            // Retrieve the ECR login password and log into ECR
            sh "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${repoUrl}"

            // Tag the image for ECR
            sh "docker tag ${fullImageTag} ${repoUrl}/${fullImageTag}"

            // Push to ECR
            sh "docker push ${repoUrl}/${fullImageTag}"
        }
    }
}
