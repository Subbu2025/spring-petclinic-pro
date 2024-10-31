def call(Map params) {
    // Ensure required parameters are passed
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('prId') : "Missing required parameter: prId"
    assert params.containsKey('awsRegion') : "Missing required parameter: awsRegion"

    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String prId = params.prId ?: "unknown"
    String buildNumber = env.BUILD_NUMBER
    String fullImageTag = "${dockerImageName}:${prId}-${buildNumber}"

    // Build Docker Image
    sh "docker build -t ${fullImageTag} ."

    // Tag and Push Docker Image to ECR
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
        // Retrieve the ECR login password and log into ECR
        sh "aws ecr get-login-password --region ${params.awsRegion} | docker login --username AWS --password-stdin ${repoUrl}"

        // Tag the image for ECR
        sh "docker tag ${fullImageTag} ${repoUrl}/${fullImageTag}"

        // Push to ECR
        sh "docker push ${repoUrl}/${fullImageTag}"
    }
}
