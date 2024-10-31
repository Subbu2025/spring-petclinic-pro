// vars/buildAndPushDocker.groovy
def call(Map params) {
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('shortCommitSha') : "Missing required parameter: shortCommitSha"

    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String prId = params.prId
    String shortCommitSha = params.shortCommitSha
    String buildNumber = env.BUILD_NUMBER ?: '0'

    // Updated ECR image tag to include the PR ID, commit SHA, and build number
    String imageTag = "${prId}-${shortCommitSha}-build-${buildNumber}"
    String ecrImageTag = "${repoUrl}:${imageTag}"

    // Build Docker Image
    sh "docker build -t ${ecrImageTag} ."

    // Push Docker Image to ECR
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
        sh "aws ecr get-login-password --region ${params.awsRegion} | docker login --username AWS --password-stdin ${repoUrl}"
        sh "docker push ${ecrImageTag}"
    }

    echo "Successfully built and pushed Docker image: ${ecrImageTag}"
}
