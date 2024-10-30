// checkoutCode.groovy
def call(Map params) {
    String branch = params.get('branch', 'main')
    String credentialsId = params.get('credentialsId', 'Subbu2025_github-creds')

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
