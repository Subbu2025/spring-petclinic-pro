def call(Map params) {
    // Parameter assertions for validation
    assert params.containsKey('repoUrl') : "Missing required parameter: repoUrl"
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('environment') : "Missing required parameter: environment"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"

    // Explicitly declare variables for each parameter for clarity
    String repoUrl = params.repoUrl
    String credentialsId = params.credentialsId
    String namespace = params.namespace
    String environment = params.environment
    String awsCredentialsId = params.awsCredentialsId
    String branch = params.get('branch', 'main')  // Default to 'main' if branch is not provided

    // Checkout the config repo
    checkout([
        $class: 'GitSCM',
        branches: [[name: branch]],
        userRemoteConfigs: [[
            url: repoUrl,
            credentialsId: credentialsId
        ]]
    ])

    // Apply ConfigMap and Secret
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: awsCredentialsId]]) {
        sh "kubectl apply -f ${environment}/configmaps/ -n ${namespace} --validate=false"
        sh "kubectl apply -f ${environment}/secrets/ -n ${namespace} --validate=false"
    }
}
