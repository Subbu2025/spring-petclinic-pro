def call(Map params = [:]) {
    String testCommand = params.get('testCommand', './mvnw test') // Default to './mvnw test' if no command is passed

    stage('Unit Testing') {
        sh testCommand
    }
}
