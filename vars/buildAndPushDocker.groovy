def call(Map params) {
    // Ensure required parameters are passed
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('prId') : "Missing required parameter: prId"
    assert params.containsKey('awsRegion') : "Missing required parameter: awsRegion"

    String repoUrl = params.repoUrl // Only the ECR repo URL without nested image name path
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String prId = params.prId ?: "unknown"
    String buildNumber = env.BUILD_NUMBER
    String fullImageTag = "${repoUrl}:${prId}-${buildNumber}" // Tag directly with repo URL and PR ID

    // Build Docker Image
    sh "docker build -t ${fullImageTag} ."

    // Login to ECR and Push Docker Image
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
        // Authenticate with ECR
        sh "aws ecr get-login-password --region ${params.awsRegion} | docker login --username AWS --password-stdin ${repoUrl}"

        // Push the Docker image to ECR
        sh "docker push ${fullImageTag}"
    }
}
