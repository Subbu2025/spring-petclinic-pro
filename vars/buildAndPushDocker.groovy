// vars/buildAndPushDocker.groovy

def call(Map params) {
    // Ensure required parameters are passed
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"

    // Retrieve parameters and Jenkins environment variables
    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String branchName = env.GIT_BRANCH?.replaceAll('/', '-') ?: 'unknown'
    String shortCommitSha = env.GIT_COMMIT?.take(7) ?: 'unknown'
    String buildNumber = env.BUILD_NUMBER ?: '0'

    // Generate the ECR-compatible image tag
    String imageTag = "${branchName}-${shortCommitSha}-build-${buildNumber}"
    String ecrImageTag = "${repoUrl}:${imageTag}"

    // Build Docker Image
    stage("Build Docker Image") {
        sh "docker build -t ${ecrImageTag} ."
    }

    // Tag and Push Docker Image to ECR
    stage("Push Docker Image to ECR") {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
            // Log in to ECR
            sh "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${repoUrl}"
            
            // Push the image to ECR
            sh "docker push ${ecrImageTag}"
        }
    }

    echo "Successfully built and pushed Docker image: ${ecrImageTag}"
}
