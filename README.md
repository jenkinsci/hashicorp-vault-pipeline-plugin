# Hashicorp Vault Pipeline plugin

Enables the use of vault from within a pipeline.

### Example

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
