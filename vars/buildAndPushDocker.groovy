def call(Map params) {
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

    // Build Docker Image
    sh "docker build -t ${fullImageTag} ."

    // Tag and Push Docker Image to ECR
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
        sh "aws ecr get-login-password --region ${params.awsRegion} | docker login --username AWS --password-stdin ${repoUrl}"
        sh "docker tag ${fullImageTag} ${repoUrl}/${fullImageTag}"
        sh "docker push ${repoUrl}/${fullImageTag}"
    }
}
