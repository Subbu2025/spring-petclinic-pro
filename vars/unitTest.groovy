def call(Map params = [:]) {
    String testCommand = params.get('testCommand', './mvnw test') // Default to './mvnw test' if no command is passed
    String stageName = params.get('stageName', 'Unit Testing')    // Default stage name

    stage(stageName) {
        sh testCommand
    }
}
