def call(Map params = [:]) {
    String testCommand = params.get('testCommand', './mvnw test')
    String stageName = params.get('stageName', 'Unit Testing')

    echo "Running tests with command: ${testCommand}"
    sh testCommand
}
