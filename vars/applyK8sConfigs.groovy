// vars/applyK8sConfigs.groovy

def call(Map params) {
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('environment') : "Missing required parameter: environment"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"

    String repoUrl = params.repoUrl
    String credentialsId = params.credentialsId
    String namespace = params.namespace
    String environment = params.environment
    String awsCredentialsId = params.awsCredentialsId

    stage("Checkout Config Repo for ${environment}") {
        git url: repoUrl, credentialsId: credentialsId
    }

    stage("Apply ConfigMap and Secret for ${environment}") {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
            // Applying ConfigMap
            sh "kubectl apply -f ${environment}/configmaps/ -n ${namespace} --validate=false"
            
            // Applying Secrets
            sh "kubectl apply -f ${environment}/secrets/ -n ${namespace} --validate=false"
        }
    }
}
