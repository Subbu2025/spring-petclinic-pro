def call(String branch = 'main') {
    stage('Checkout Code') {
        checkout([
            $class: 'GitSCM',
            branches: [[name: main]],
            userRemoteConfigs: [[url: 'https://github.com/Subbu2025/spring-petclinic-pro.git']]
        ])
    }
}
