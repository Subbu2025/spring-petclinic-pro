// vars/buildAndPushDocker.groovy

def call(Map params) {
    // Ensure required parameters are passed
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('awsRegion') : "Missing required parameter: awsRegion"

    // Retrieve parameters and Jenkins environment variables
    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String awsRegion = params.awsRegion
    String branchName = env.GIT_BRANCH?.replaceAll('/', '-') ?: 'dev'  // Default to 'dev' if not found
    String shortCommitSha = env.GIT_COMMIT?.take(7) ?: 'latest'  // Default to 'latest' if not found
    String buildNumber = env.BUILD_NUMBER ?: '0'

    // Generate the ECR-compatible image tag
    String imageTag = "${branchName}-${shortCommitSha}-build-${buildNumber}"
    String ecrImageTag = "${repoUrl}:${imageTag}"

    // Build Docker Image
        sh "docker build -t ${ecrImageTag} ."
    

    // Push Docker Image to ECR
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
            // Log in to ECR using the specified region
            sh "aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${repoUrl}"
            
            // Push the image to ECR
            sh "docker push ${ecrImageTag}"
        }
    

    echo "Successfully built and pushed Docker image: ${ecrImageTag}"
}
