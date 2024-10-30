def call(Map params = [:]) {
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"
    assert params.containsKey('url') : "Missing required parameter: url"

    String branch = params.get('branch', 'main')
    String credentialsId = params.credentialsId
    String repoUrl = params.url
    String stageName = params.get('stageName', 'Checkout Code') // Allow custom stage names

    stage(stageName) {
        echo "Checking out branch '${branch}' from '${repoUrl}'"
        
        checkout([
            $class: 'GitSCM',
            branches: [[name: branch]],
            userRemoteConfigs: [[
                url: repoUrl,
                credentialsId: credentialsId
            ]]
        ])
    }
}
