def call(Map params = [:]) {
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

    String checkoutStageName = params.get('checkoutStageName', "Checkout Config Repo for ${environment}")
    String applyStageName = params.get('applyStageName', "Apply ConfigMap and Secret for ${environment}")

    // Checkout Config Repo Stage
    stage(checkoutStageName) {
        checkout([
            $class: 'GitSCM',
            branches: [[name: params.branch ?: 'main']],
            userRemoteConfigs: [[
                url: repoUrl,
                credentialsId: credentialsId
            ]]
        ])
    }

    // Apply ConfigMap and Secret Stage
    stage(applyStageName) {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
            sh "kubectl apply -f ${environment}/configmaps/ -n ${namespace} --validate=false"
            sh "kubectl apply -f ${environment}/secrets/ -n ${namespace} --validate=false"
        }
    }
}
