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
        SECRET = vault path: 'secrets', key: 'username'
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
        SECRET = vault path: 'secrets', key: 'username', vaultUrl: 'https://my-vault.com:8200', credentialsId: 'my-creds'
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
