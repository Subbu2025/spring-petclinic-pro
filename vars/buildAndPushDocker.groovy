def call(Map params = [:]) {
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('awsRegion') : "Missing required parameter: awsRegion"
    assert params.containsKey('prId') : "Missing required parameter: prId"

    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String awsRegion = params.awsRegion
    String prId = params.prId ?: 'unknown'
    String buildNumber = env.BUILD_NUMBER
    String fullImageTag = "${dockerImageName}:${prId}-${buildNumber}"

    dir('application') {
        stage("Build Docker Image") {
            sh "docker build -t ${fullImageTag} ."
        }

        stage("Tag and Push Docker Image to ECR") {
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
                // Login to ECR
                sh "aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${repoUrl}"

                // Tag and push the Docker image
                sh "docker tag ${fullImageTag} ${repoUrl}/${dockerImageName}:${prId}-${buildNumber}"
                sh "docker push ${repoUrl}/${dockerImageName}:${prId}-${buildNumber}"
            }
        }
    }
}
