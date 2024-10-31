// vars/checkoutCode.groovy
def call(Map params) {
    assert params.containsKey('url') : "Missing required parameter: url"
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"

    String branch = params.get('branch', 'main')

    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        userRemoteConfigs: [[
            url: params.url,
            credentialsId: params.credentialsId
        ]]
    ])
    
    // Retrieve the commit SHA after checkout
    env.shortCommitSha = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    echo "Retrieved Commit SHA: ${env.shortCommitSha}"
}
