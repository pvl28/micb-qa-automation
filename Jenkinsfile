// Declarative pipeline for micb-qa-automation.
// Jenkins prerequisites (Manage Jenkins -> Tools): a JDK named 'jdk21' and Maven named 'maven3';
// Google Chrome installed on the agent. Plugins: Pipeline, JUnit, HTML Publisher.
// Optional: Xray import stage needs Jenkins credentials 'xray-client-id' / 'xray-client-secret'.

pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    parameters {
        choice(name: 'SUITE',
               choices: ['@smoke', '@regression', '@api', '@ui', 'not @wip'],
               description: 'Cucumber tag expression to run (not @wip = everything)')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Browser for @ui scenarios')
        booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Run the browser without a window')
        booleanParam(name: 'XRAY_IMPORT', defaultValue: false,
                     description: 'Import Cucumber JSON into Xray Cloud (requires credentials and a project key)')
        string(name: 'XRAY_PROJECT_KEY', defaultValue: '', description: 'Jira project key for the Test Execution, e.g. QA')
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        // Nightly runs and manual runs never overlap: keeps load on the public site low
        disableConcurrentBuilds()
    }

    triggers {
        // Nightly smoke on weekdays; regression is started manually
        cron('H 6 * * 1-5')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -ntp clean test-compile'
            }
        }

        stage('Test') {
            steps {
                // A failing scenario marks the build UNSTABLE (yellow), not FAILED (red):
                // the pipeline still publishes reports. Compilation errors still fail the build.
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    sh """
                        mvn -B -ntp test \
                          -Dcucumber.filter.tags='${params.SUITE}' \
                          -Dbrowser=${params.BROWSER} \
                          -Dheadless=${params.HEADLESS}
                    """
                }
            }
        }

        stage('Xray import') {
            when {
                expression { params.XRAY_IMPORT && params.XRAY_PROJECT_KEY?.trim() }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'xray-client-id', variable: 'XRAY_CLIENT_ID'),
                    string(credentialsId: 'xray-client-secret', variable: 'XRAY_CLIENT_SECRET')
                ]) {
                    sh "XRAY_PROJECT_KEY='${params.XRAY_PROJECT_KEY}' scripts/xray-import-cloud.sh target/cucumber-reports/cucumber.json"
                }
            }
        }
    }

    post {
        always {
            // Test results -> Jenkins "Test Result" trend (one entry per scenario)
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

            // Human-readable Cucumber report with screenshots embedded for failed UI scenarios
            publishHTML(target: [
                reportName           : 'Cucumber Report',
                reportDir            : 'target/cucumber-reports',
                reportFiles          : 'cucumber.html',
                keepAll              : true,
                alwaysLinkToLastBuild: true,
                allowMissing         : true
            ])

            archiveArtifacts artifacts: 'target/cucumber-reports/**, target/screenshots/**, target/logs/**',
                             allowEmptyArchive: true,
                             fingerprint: false
        }
    }
}
