def call(Map params = [:]) {
    assert params.containsKey('namespace') : "Missing required parameter: namespace"
    assert params.containsKey('awsCredentialsId') : "Missing required parameter: awsCredentialsId"

    String namespace = params.namespace
    String sonarUrl
    String sonarProjectKey
    String sonarToken

    stage("SonarQube Analysis") {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: params.awsCredentialsId]]) {
            // Retrieve SonarQube URL and Project Key from the ConfigMap
            sonarUrl = sh(script: "kubectl get configmap sonar-config -n ${namespace} -o=jsonpath='{.data.SONAR_URL}'", returnStdout: true).trim()
            sonarProjectKey = sh(script: "kubectl get configmap sonar-config -n ${namespace} -o=jsonpath='{.data.SONAR_PROJECT_KEY}'", returnStdout: true).trim()

            // Retrieve SonarQube token from the Secret
            sonarToken = sh(script: "kubectl get secret sonar-secrets -n ${namespace} -o=jsonpath='{.data.SONAR_TOKEN}' | base64 --decode", returnStdout: true).trim()

            // Debugging step: List contents of the workspace
            sh "ls -la"

            // Run SonarQube analysis
            withEnv(["SONAR_HOST_URL=${sonarUrl}", "SONAR_PROJECT_KEY=${sonarProjectKey}", "SONAR_TOKEN=${sonarToken}"]) {
                sh "./mvnw sonar:sonar -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.login=${SONAR_TOKEN}"
            }
        }
    }
}
