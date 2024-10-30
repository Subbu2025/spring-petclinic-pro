def call(Map params = [:]) {
    // Validate required parameters
    assert params.containsKey('credentialsId') : "Missing required parameter: credentialsId"
    assert params.containsKey('url') : "Missing required parameter: url"

    // Set defaults for optional parameters
    String branch = params.get('branch', 'main')
    String credentialsId = params.get('credentialsId')
    String repo_url = params.get('url')

    stage('Checkout Code') {
        echo "Checking out branch '${branch}' from '${repo_url}'"
        
        checkout([
            $class: 'GitSCM',
            branches: [[name: branch]],
            userRemoteConfigs: [[
                url: repo_url,
                credentialsId: credentialsId
            ]]
        ])
    }
}
