// vars/applyK8sConfigs.groovy

def call(Map params) {
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('environment') : "Missing required parameter: environment"

    String repoUrl = params.repoUrl
    String credentialsId = params.credentialsId
    String namespace = params.namespace
    String environment = params.environment

    stage("Checkout Config Repo for ${environment}") {
        git url: repoUrl, credentialsId: credentialsId
    }

    stage("Apply ConfigMap and Secret for ${environment}") {
        // Applying ConfigMap
        sh "kubectl apply -f ${environment}/configmaps/ -n ${namespace}"
        
        // Applying Secrets
        sh "kubectl apply -f ${environment}/secrets/ -n ${namespace}"
    }
}
