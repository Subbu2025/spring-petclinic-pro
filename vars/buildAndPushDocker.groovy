// vars/buildAndPushDocker.groovy

def call(Map params) {
    // Ensure required parameters are passed
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"
    assert params.containsKey('dockerImageName') : "Missing required parameter: dockerImageName"
    assert params.containsKey('prId') : "Missing required parameter: prId"
    
    // Optional parameter with a default value for region
    String awsRegion = params.get('awsRegion', 'us-east-1')
    
    String repoUrl = params.repoUrl
    String awsCredentialsId = params.awsCredentialsId
    String dockerImageName = params.dockerImageName
    String prId = params.prId
    String buildNumber = env.BUILD_NUMBER
    String fullImageTag = "${dockerImageName}:${prId}-${buildNumber}"

    stage("Build Docker Image") {
        echo "Building Docker image with tag ${fullImageTag}"
        sh "docker build -t ${fullImageTag} ."
    }

    stage("Tag and Push Docker Image to ECR") {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
            echo "Logging into AWS ECR and pushing image to ${repoUrl}"
            
            // ECR login
            sh "aws ecr get-login-password --region ${awsRegion} | docker login --username AWS --password-stdin ${repoUrl}"
            
            // Tagging the image for ECR
            sh "docker tag ${fullImageTag} ${repoUrl}/${fullImageTag}"
            
            // Pushing the image
            sh "docker push ${repoUrl}/${fullImageTag}"
        }
    }
    
    echo "Docker image ${repoUrl}/${fullImageTag} pushed successfully."
}
