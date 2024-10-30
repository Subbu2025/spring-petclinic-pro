// vars/applyK8sConfigs.groovy

def call(Map params) {
    // Ensure required parameters
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('environment') : "Missing required parameter: environment"

    String repoUrl = params.get('repoUrl', '')
    String credentialsId = params.get('credentialsId', '')
    String namespace = params.namespace
    String environment = params.environment

    // Conditionally check out the repo only if repoUrl and credentialsId are provided
    if (repoUrl && credentialsId) {
        stage("Checkout Config Repo for ${environment}") {
            git url: repoUrl, credentialsId: credentialsId
        }
    } else {
        echo "Using previously checked-out configuration repo for ${environment}."
    }

    stage("Apply ConfigMap and Secret for ${environment}") {
        // Applying ConfigMap
        sh "kubectl apply -f ${environment}/configmaps/ -n ${namespace}"
        
        // Applying Secrets
        sh "kubectl apply -f ${environment}/secrets/ -n ${namespace}"
    }
}
