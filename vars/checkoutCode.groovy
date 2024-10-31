// vars/checkoutCode.groovy
def call(Map params) {
    assert params.containsKey('url') : "Missing required parameter: url"
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"

    String branch = params.get('branch', 'main')

    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0, timeout: 10]],
        userRemoteConfigs: [[
            url: params.url,
            credentialsId: params.credentialsId
        ]]
    ])
}
