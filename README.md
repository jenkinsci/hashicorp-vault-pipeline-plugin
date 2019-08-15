# Hashicorp Vault Pipeline Plugin

Enables the use of vault from within a pipeline.

### Dependencies

- [hashicorp-vault-plugin](https://github.com/jenkinsci/hashicorp-vault-plugin)
- [credentials-plugin](https://github.com/jenkinsci/credentials-plugin)

### Examples

##### Using gloabl vault configuration

```
pipeline {
    agent any
    environment {
        SECRET = vault path: 'secrets', key: 'username', engineVersion: 2
    }
    stages {
        stage("read vault key") {
            steps {
                echo "${SECRET}"
            }
        }
    }
}
```

##### Using pipeline specific configuration

```
pipeline {
    agent any
    environment {
        SECRET = vault path: 'secrets', key: 'username', vaultUrl: 'https://my-vault.com:8200', credentialsId: 'my-creds', engineVersion: 2
    }
    stages {
        stage("read vault key") {
            steps {
                echo "${SECRET}"
            }
        }
    }
}
```

##### Masking secrets in console output
By default, the plugin does not hide any accidental printing of secret to console. This becomes an issue because `set -x` is set by default in pipeline, so each command with the secrets being passed in will be printed.

[Masked Password Plugin is Required](https://wiki.jenkins.io/display/JENKINS/Mask+Passwords+Plugin)

```
pipeline {
    agent any
    environment {
        SECRET1    = vault path: 'secrets', key: 'password1', vaultUrl: 'https://my-vault.com:8200', credentialsId: 'my-creds', engineVersion: 2
        SECRET2    = vault path: 'secrets', key: 'password2', vaultUrl: 'https://my-vault.com:8200', credentialsId: 'my-creds', engineVersion: 2
        NOT_SECRET = vault path: 'secrets', key: 'username', vaultUrl: 'https://my-vault.com:8200', credentialsId: 'my-creds', engineVersion: 2
    }
    stages {
        stage("read vault key") {
            steps {
              wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: env['SECRET1'], var: 'SECRET'], [password: env['SECRET2'], var: 'SECRET']]]) {
                echo "These secrets will be masked: ${SECRET1} and ${SECRET2}"
                echo "This secret will be printed in clear text: ${NOT_SECRET}"
              }
            }
        }
    }
}
```
