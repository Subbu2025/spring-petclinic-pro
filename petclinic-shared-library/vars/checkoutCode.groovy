def call(String branch = 'main', String credentialsId = 'Subbu2025_github-creds') {
    stage('Checkout Code') {
        checkout([
            $class: 'GitSCM',
            branches: [[name: branch]],
            userRemoteConfigs: [[
                url: 'https://github.com/Subbu2025/spring-petclinic-pro.git',
                credentialsId: credentialsId
            ]]
        ])
    }
}
