// dependent on templates/shortcuts/defaultBuilds.groovy

class EntityEngine
{
    // project specific settings
    static owner = 'cfuller'
    static key = 'OFBEE'
    static name = 'OfBiz Entity Engine Fork'
    static repository = 'entity-engine'

    static includeDefaults(configuration)
    {
        // Insert defaults into configuration
        [
                owner : owner,
                projectKey : key,
                projectName : name,
                planDescription: "${name} - ${configuration.planName}",
                sharedRepoName : repository,
                enableJdk16BuildStage : 'true',
                enableJdk17BuildStage : 'true',
                enableJdk18BuildStage : 'true',
                enableReleaseStage : 'true',
                jobType : 'maven3',
                jobMavenExe : 'Maven 3.2',
                jobGoal : 'clean verify',
                label : 'ofbiz',
                releaseJavaVersion : 'JDK 1.6',
                hasTests : 'true',
        ].each { key, value -> configuration.get(key, value) }
        // Stringify everything in to evaluate GString to String
        configuration.each { key, value -> configuration[key] = value.toString() }
    }
}

templateRepository(url: "https://bitbucket.org/atlassian/entity-engine")

// maven 3
[ '1.0' ].each
        { projectBranch ->
            defaultPlan(EntityEngine.includeDefaults(
                    planKey : "${EntityEngine.key}B${projectBranch.replace('.','D')}X",
                    planName : "${projectBranch}.x",
                    branch : "${EntityEngine.repository}-${projectBranch}.x",
                    issueSuffix : "-${projectBranch}",
            ))
        }

defaultPlan(EntityEngine.includeDefaults(
        planKey : "${EntityEngine.key}M",
        planName : 'master',
        branch : 'master',
        issueSuffix : '',
))
