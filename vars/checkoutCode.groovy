def call(Map params) {
    String branch = params.get('branch', 'main')
    String credentialsId = params.get('credentialsId')
    String repo_url = params.get('url') 

    stage('Checkout Code') {
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
