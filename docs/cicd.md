# CICD Process

A new build job can be triggered either manually or when a new git commit is pushed. 

When job started, Gradle version set in `build.yaml` file is being used to setup Gradle environment. When job finishes, 
the build artifacts are uploaded to Github and become Job Artifacts.

The build pipeline is using Pipeline Strategy Matrix to allow us to run parallel jobs and each job has its own configuration.

In the following example, two configurations are specified and there will be two jobs created.

```yaml
strategy:
  matrix:
    profile: [
      {
        java_source_version: "21",
        java_target_version: "17",
        elasticsearch_version: "8.15.0"
      },
      {
        java_source_version: "21",
        java_target_version: "21",
        elasticsearch_version: "8.15.0"
      }
    ]
```
<br>

When build job is triggered on `main` branch, additional `release` job will be executed. The release job perform a gradle release
using [Gradle Release Plugin](https://github.com/researchgate/gradle-release). Project version number will have `minor` 
version incremented after the release. This is controlled by the following configuration

```
versionPatterns = [
        /(\d+)\.(\d+)\.(\d+)(.*)/ : { Matcher m, Project p ->
            def major = m.group(1) as int
            def minor = (m.group(2) as int) + 1
            def patch = 0
            def suffix = m.group(4)
            "${major}.${minor}.${patch}${suffix}"
        }
]
```

<br>

A new Git tag and Github release will also be created after the release job is completed.

<br>
