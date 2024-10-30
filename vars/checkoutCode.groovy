def call(Map params) {
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"
    assert params.containsKey('url') : "Missing required parameter: url"

    String branch = params.get('branch', 'main')
    String credentialsId = params.get('credentialsId')
    String repoUrl = params.get('url')
    String stageName = params.get('stageName', 'Checkout Code')

    // Only echo details, no `stage(stageName)` here
    echo "Checking out branch '${branch}' from '${repoUrl}'"
    
    checkout([
        $class: 'GitSCM',
        branches: [[name: branch]],
        userRemoteConfigs: [[url: repoUrl, credentialsId: credentialsId]]
    ])
}
