def call(Map params) {
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('environment') : "Missing required parameter: environment"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"

    // Checkout the config repo
    checkout([
        $class: 'GitSCM',
        branches: [[name: params.branch ?: 'main']],
        userRemoteConfigs: [[
            url: params.repoUrl,
            credentialsId: params.credentialsId
        ]]
    ])

    // Apply ConfigMap and Secret
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: params.awsCredentialsId]]) {
        sh "kubectl apply -f ${params.environment}/configmaps/ -n ${params.namespace} --validate=false"
        sh "kubectl apply -f ${params.environment}/secrets/ -n ${params.namespace} --validate=false"
    }
}
