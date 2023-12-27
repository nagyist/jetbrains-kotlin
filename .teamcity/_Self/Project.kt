package _Self

import _Self.buildTypes.*
import _Self.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.CustomChart
import jetbrains.buildServer.configs.kotlin.CustomChart.*
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudImage
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudProfile
import jetbrains.buildServer.configs.kotlin.buildTypeCustomChart
import jetbrains.buildServer.configs.kotlin.kubernetesCloudImage
import jetbrains.buildServer.configs.kotlin.kubernetesCloudProfile
import jetbrains.buildServer.configs.kotlin.projectCustomChart

object Project : Project({
    description = "Kotlin Compiler, IDEA plugin and tests: mainline development branches"

    vcsRoot(CommunityProjectPluginVCS)
    vcsRoot(TeamCityBuild)
    vcsRoot(coroutinesForAggregateVCS)
    vcsRoot(SerializationVCS)
    vcsRoot(CompatibleComposeVCS)
    vcsRoot(IntellijMonorepoForKotlinVCS_kt_master)

    buildType(KotlinxLibrariesCompilation)
    buildType(CompilerDist)
    buildType(ValidateIdePluginDependencies)
    buildType(MavenArtifactsAgent)
    buildType(CompilerDistLocal)
    buildType(CompilerDistAndMavenArtifacts)
    buildType(KotlinNativeSanityRemoteRunComposite)
    buildType(ComposeArtifacts)
    buildType(CompileAllClasses)
    buildType(Nightly)
    buildType(CompilerDistLocalOverrideObsoleteJdk)
    buildType(BuildGradleIntegrationTests)
    buildType(LibraryReferenceLatestDocs)
    buildType(SafeMerge)
    buildType(IdePluginsAggregate)
    buildType(CompilerDistAndMavenArtifactsForIde)
    buildType(LibraryReferenceLegacyDocs)
    buildType(Aggregate)
    buildType(CompilerArtifactsInDocker)
    buildType(NightlyCritical)
    buildType(CompilerArtifacts)
    buildType(CompilerDistLocalK2)
    buildType(BuildNumber)
    buildType(Artifacts)
    buildType(MavenArtifactsDocker)
    buildType(ResolveDependencies)

    params {
        param("build.number.native.meta.version", "titan")
        param("teamcity.internal.gradle.runner.launch.mode", "gradle-tooling-api")
        text("env.GRADLE_ENTERPRISE_ACCESS_KEY", "ge.jetbrains.com=%gradle.enterprise.access.key%")
        param("teamcity.internal.git.sshDebug", "true")
        param("teamcity.internal.webhooks.events", "BUILD_FINISHED")
        param("globalGradleParameters", """-Pteamcity=true --info --full-stacktrace "-Pbuild.number=%build.number%" %globalGradleCacheNodeParameters% %globalGradleBuildScanParameters%""")
        param("teamcity.internal.webhooks.url", "https://3z66zlayx9.execute-api.eu-west-1.amazonaws.com/1")
        param("build.number.prefix", "2.0.0-titan")
        param("teamcity.internal.webhooks.enable", "false")
        param("globalGradleCacheNodeParameters", " -Pkotlin.build.cache.url=https://gradle-cache.kotlin.intellij.net/cache/ -Pkotlin.build.cache.user=%kotlin.build.cache.user% -Pkotlin.build.cache.password=%kotlin.build.cache.password% -Pkotlin.build.cache.push=true")
        text("teamcity.activeVcsBranch.age.days", "5", display = ParameterDisplay.HIDDEN)
        param("globalGradleBuildScanParameters", "")
    }

    features {
        projectCustomChart {
            id = "KotlinCompilerQueueWaitReasonTime"
            title = "Kotlin Compiler Queue Wait Reason Time"
            seriesTitle = "Reason"
            format = CustomChart.Format.DURATION
            series = listOf(
                Serie(title = "Total: Time Spent in Queue", key = SeriesKey.QUEUE_TIME, sourceBuildTypeId = "Kotlin_BuildPlayground_Titan_CompilerDistAndMavenArtifacts"),
                Serie(title = "Agent: All compatible agents are outdated - waiting for upgrade", key = SeriesKey("queueWaitReason:All_compatible_agents_are_outdated__waiting_for_upgrade"), sourceBuildTypeId = "Kotlin_BuildPlayground_Titan_CompilerDistAndMavenArtifacts"),
                Serie(title = "Agent: Waiting for starting agent", key = SeriesKey("queueWaitReason:Waiting_for_starting_agent"), sourceBuildTypeId = "Kotlin_BuildPlayground_Titan_CompilerDistAndMavenArtifacts"),
                Serie(title = "Agent: There are no compatible agents which can run this build", key = SeriesKey("queueWaitReason:There_are_no_compatible_agents_which_can_run_this_build"), sourceBuildTypeId = "Kotlin_BuildPlayground_Titan_CompilerDistAndMavenArtifacts"),
                Serie(title = "Dependencies: Build dependencies have not been built yet", key = SeriesKey("queueWaitReason:Build_dependencies_have_not_been_built_yet"), sourceBuildTypeId = "Kotlin_BuildPlayground_Titan_CompilerDistAndMavenArtifacts")
            )
        }
        projectCustomChart {
            id = "KotlinMethodCount"
            title = "Method count"
            seriesTitle = "Serie"
            format = CustomChart.Format.INTEGER
            series = listOf(
                Serie(title = "kotlin-stdlib", key = SeriesKey("DexMethodCount_kotlin-stdlib"), sourceBuildTypeId = "Kotlin_BuildPlayground_Titan_CompilerDistAndMavenArtifacts"),
                Serie(title = "kotlin-reflect", key = SeriesKey("DexMethodCount_kotlin-reflect"), sourceBuildTypeId = "Kotlin_BuildPlayground_Titan_CompilerDistAndMavenArtifacts")
            )
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_1"
            profileId = "titan-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "titan-kotlin-k8s-kmp-test-macos"
            maxInstancesCount = 2
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kmm-macos-arm64-agent:xcode-15.1
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 4
                              memory: 8Gi
                            limits:
                              cpu: 7
                              memory: 8Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        buildTypeCustomChart {
            id = "PROJECT_EXT_165"
            title = "Import duration"
            seriesTitle = "Serie"
            format = CustomChart.Format.TEXT
            series = listOf(
                Serie(title = "import_duration", key = SeriesKey("import_duration"))
            )
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_2"
            profileId = "titan-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "titan-kotlin-k8s-native-intel-test-macos"
            maxInstancesCount = 1
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kotlin-native-macos-arm64-agent:xcode-15.1
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            arch -x86_64 /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_3"
            profileId = "titan-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "titan-kotlin-k8s-native-xcode-stable-macos"
            maxInstancesCount = 10
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kotlin-native-arm64-xcode-stable-agent:latest
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        kubernetesCloudImage {
            id = "PROJECT_EXT_4"
            profileId = "titan-kotlin-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "titan-kotlin-k8s-native-xcode-beta-macos"
            maxInstancesCount = 10
            podSpecification = customTemplate {
                customPod = """
                    ---
                    apiVersion: v1
                    kind: Pod
                    metadata:
                      # name: would be set as %agent-name-prefix%-%id%
                      # namespace: would be set from cloud-profile
                      labels:
                        agent-id: %instance.id%
                    spec:
                      enableServiceLinks: false
                      securityContext:
                        runAsUser: 20 # =admin
                      containers:
                        - name: macos
                          image: mac-registry.eqx.k8s.intellij.net/kotlin/kotlin-native-arm64-xcode-beta-agent:latest
                          imagePullPolicy: Always
                          resources:
                            requests:
                              cpu: 7
                              memory: 16Gi
                            limits:
                              cpu: 7
                              memory: 16Gi
                          command:
                          - "bash"
                          - "-c"
                          - |
                            /Users/admin/agent-launcher.sh
                """.trimIndent()
            }
        }
        amazonEC2CloudProfile {
            id = "titan-aws"
            name = "titan-aws"
            terminateIdleMinutes = 30
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudProfile {
            id = "titan-aws-on-demand"
            name = "titan-aws-on-demand"
            terminateIdleMinutes = 30
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudProfile {
            id = "titan-gradle-daemon"
            name = "titan-gradle-daemon"
            terminateIdleMinutes = 180
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudImage {
            id = "titan-gradle-daemon-linux-8cpu-16gb"
            profileId = "titan-gradle-daemon"
            agentPoolId = "-2"
            name = "titan-gradle-daemon-linux-8cpu-16gb"
            maxInstancesCount = 40
            source = SpotFleetConfig("""
                {
                                      "IamFleetRole": "arn:aws:iam::043385162245:role/aws-ec2-spot-fleet-tagging-role",
                                      "AllocationStrategy": "capacityOptimized",
                                      "LaunchSpecifications": [
                                        {
                  "ImageId": "ami-099af2c45059cc887",
                  "InstanceType": "c5d.2xlarge",
                  "SubnetId": "subnet-005cb0d3007eac8df, subnet-0395763caa3d34938, subnet-0b4669ea24547e823",
                  "KeyName": "kotlin-private-tc-agents-2021",
                  "SecurityGroups": [
                    {
                      "GroupId": "sg-0e018ed62129c956d"
                    }
                  ],
                  "TagSpecifications": [
                    {
                      "ResourceType": "instance",
                      "Tags": [
                        {
                          "Key": "Name",
                          "Value": "buildserver agent"
                        },
                        {
                          "Key": "Image",
                          "Value": "titan-gradle-daemon-linux-8cpu-16gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-gradle-daemon"
                        }
                      ]
                    }
                  ],
                  "IamInstanceProfile": {
                    "Arn": "arn:aws:iam::043385162245:instance-profile/BuildAgent"
                  }
                },
                                        {
                  "ImageId": "ami-099af2c45059cc887",
                  "InstanceType": "c5ad.2xlarge",
                  "SubnetId": "subnet-005cb0d3007eac8df, subnet-0395763caa3d34938, subnet-0b4669ea24547e823",
                  "KeyName": "kotlin-private-tc-agents-2021",
                  "SecurityGroups": [
                    {
                      "GroupId": "sg-0e018ed62129c956d"
                    }
                  ],
                  "TagSpecifications": [
                    {
                      "ResourceType": "instance",
                      "Tags": [
                        {
                          "Key": "Name",
                          "Value": "buildserver agent"
                        },
                        {
                          "Key": "Image",
                          "Value": "titan-gradle-daemon-linux-8cpu-16gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-gradle-daemon"
                        }
                      ]
                    }
                  ],
                  "IamInstanceProfile": {
                    "Arn": "arn:aws:iam::043385162245:instance-profile/BuildAgent"
                  }
                }
                                      ]
                                    }
            """.trimIndent())
        }
        kubernetesCloudProfile {
            id = "titan-kotlin-k8s"
            name = "titan-kotlin-k8s"
            terminateAfterBuild = true
            terminateIdleMinutes = 30
            apiServerURL = "https://eqx.k8s.intellij.net:6443"
            namespace = "agents-kotlin"
            authStrategy = token {
                token = "credentialsJSON:3f7bc0d8-6183-4618-8587-8956091bd49c"
            }
        }
        amazonEC2CloudImage {
            id = "titan-linux-4cpu-16gb"
            profileId = "titan-aws"
            agentPoolId = "-2"
            name = "titan-linux-4cpu-16gb"
            maxInstancesCount = 400
            source = SpotFleetConfig("""
                {
                                      "IamFleetRole": "arn:aws:iam::043385162245:role/aws-ec2-spot-fleet-tagging-role",
                                      "LaunchSpecifications": [
                                        {
                  "ImageId": "ami-099af2c45059cc887",
                  "InstanceType": "m5d.xlarge",
                  "SubnetId": "subnet-005cb0d3007eac8df, subnet-0395763caa3d34938, subnet-0b4669ea24547e823",
                  "KeyName": "kotlin-private-tc-agents-2021",
                  "SecurityGroups": [
                    {
                      "GroupId": "sg-0e018ed62129c956d"
                    }
                  ],
                  "TagSpecifications": [
                    {
                      "ResourceType": "instance",
                      "Tags": [
                        {
                          "Key": "Name",
                          "Value": "buildserver agent"
                        },
                        {
                          "Key": "Image",
                          "Value": "titan-linux-4cpu-16gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-aws"
                        }
                      ]
                    }
                  ],
                  "IamInstanceProfile": {
                    "Arn": "arn:aws:iam::043385162245:instance-profile/BuildAgent"
                  }
                }
                                      ]
                                    }
            """.trimIndent())
        }
        amazonEC2CloudImage {
            id = "titan-linux-8cpu-16gb"
            profileId = "titan-aws"
            agentPoolId = "-2"
            name = "titan-linux-8cpu-16gb"
            maxInstancesCount = 400
            source = SpotFleetConfig("""
                {
                                      "IamFleetRole": "arn:aws:iam::043385162245:role/aws-ec2-spot-fleet-tagging-role",
                                      "LaunchSpecifications": [
                                        {
                  "ImageId": "ami-099af2c45059cc887",
                  "InstanceType": "c5d.2xlarge",
                  "SubnetId": "subnet-005cb0d3007eac8df, subnet-0395763caa3d34938, subnet-0b4669ea24547e823",
                  "KeyName": "kotlin-private-tc-agents-2021",
                  "SecurityGroups": [
                    {
                      "GroupId": "sg-0e018ed62129c956d"
                    }
                  ],
                  "TagSpecifications": [
                    {
                      "ResourceType": "instance",
                      "Tags": [
                        {
                          "Key": "Name",
                          "Value": "buildserver agent"
                        },
                        {
                          "Key": "Image",
                          "Value": "titan-linux-8cpu-16gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-aws"
                        }
                      ]
                    }
                  ],
                  "IamInstanceProfile": {
                    "Arn": "arn:aws:iam::043385162245:instance-profile/BuildAgent"
                  }
                },
                                        {
                  "ImageId": "ami-099af2c45059cc887",
                  "InstanceType": "c5ad.2xlarge",
                  "SubnetId": "subnet-005cb0d3007eac8df, subnet-0395763caa3d34938, subnet-0b4669ea24547e823",
                  "KeyName": "kotlin-private-tc-agents-2021",
                  "SecurityGroups": [
                    {
                      "GroupId": "sg-0e018ed62129c956d"
                    }
                  ],
                  "TagSpecifications": [
                    {
                      "ResourceType": "instance",
                      "Tags": [
                        {
                          "Key": "Name",
                          "Value": "buildserver agent"
                        },
                        {
                          "Key": "Image",
                          "Value": "titan-linux-8cpu-16gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-aws"
                        }
                      ]
                    }
                  ],
                  "IamInstanceProfile": {
                    "Arn": "arn:aws:iam::043385162245:instance-profile/BuildAgent"
                  }
                }
                                      ]
                                    }
            """.trimIndent())
        }
        amazonEC2CloudImage {
            id = "titan-linux-8cpu-32gb"
            profileId = "titan-aws"
            agentPoolId = "-2"
            name = "titan-linux-8cpu-32gb"
            maxInstancesCount = 200
            source = SpotFleetConfig("""
                {
                                      "IamFleetRole": "arn:aws:iam::043385162245:role/aws-ec2-spot-fleet-tagging-role",
                                      "LaunchSpecifications": [
                                        {
                  "ImageId": "ami-099af2c45059cc887",
                  "InstanceType": "m5d.2xlarge",
                  "SubnetId": "subnet-005cb0d3007eac8df, subnet-0395763caa3d34938, subnet-0b4669ea24547e823",
                  "KeyName": "kotlin-private-tc-agents-2021",
                  "SecurityGroups": [
                    {
                      "GroupId": "sg-0e018ed62129c956d"
                    }
                  ],
                  "TagSpecifications": [
                    {
                      "ResourceType": "instance",
                      "Tags": [
                        {
                          "Key": "Name",
                          "Value": "buildserver agent"
                        },
                        {
                          "Key": "Image",
                          "Value": "titan-linux-8cpu-32gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-aws"
                        }
                      ]
                    }
                  ],
                  "IamInstanceProfile": {
                    "Arn": "arn:aws:iam::043385162245:instance-profile/BuildAgent"
                  }
                }
                                      ]
                                    }
            """.trimIndent())
        }
        amazonEC2CloudImage {
            id = "titan-on-demand-windows-4cpu-16gb-a"
            profileId = "titan-aws-on-demand"
            agentPoolId = "-2"
            name = "titan-on-demand-windows-4cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-07080aba505aa7ebf"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "titan-on-demand-windows-4cpu-16gb-b"
            profileId = "titan-aws-on-demand"
            agentPoolId = "-2"
            name = "titan-on-demand-windows-4cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-07080aba505aa7ebf"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "titan-on-demand-windows-4cpu-16gb-c"
            profileId = "titan-aws-on-demand"
            agentPoolId = "-2"
            name = "titan-on-demand-windows-4cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "m5d.xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-07080aba505aa7ebf"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "titan-on-demand-windows-8cpu-16gb-a"
            profileId = "titan-aws-on-demand"
            agentPoolId = "-2"
            name = "titan-on-demand-windows-8cpu-16gb-a"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-07080aba505aa7ebf"
            source = LaunchTemplate(templateId = "lt-0c4b119ddc5c4c0cd", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "titan-on-demand-windows-8cpu-16gb-b"
            profileId = "titan-aws-on-demand"
            agentPoolId = "-2"
            name = "titan-on-demand-windows-8cpu-16gb-b"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-07080aba505aa7ebf"
            source = LaunchTemplate(templateId = "lt-05363fe22d228d055", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
        amazonEC2CloudImage {
            id = "titan-on-demand-windows-8cpu-16gb-c"
            profileId = "titan-aws-on-demand"
            agentPoolId = "-2"
            name = "titan-on-demand-windows-8cpu-16gb-c"
            vpcSubnetId = "__TEMPLATE__VALUE__"
            iamProfile = "__TEMPLATE__VALUE__"
            keyPairName = "kotlin-private-tc-agents-2021"
            instanceType = "c5d.2xlarge"
            securityGroups = listOf("__TEMPLATE__VALUE__")
            maxInstancesCount = 100
            customizeLaunchTemplate = true
            launchTemplateCustomAmi = "ami-07080aba505aa7ebf"
            source = LaunchTemplate(templateId = "lt-0247190adbe8bf152", version = AmazonEC2CloudImage.LATEST_VERSION)
        }
    }

    cleanup {
        keepRule {
            id = "Keep Logs and Statistics for a month"
            keepAtLeast = days(31)
            applyToBuilds {
                inBranches {
                    branchFilter = patterns("+:<default>")
                }
            }
            dataToKeep = historyAndStatistics {
                preserveLogs = true
            }
        }
        baseRule {
            artifacts(builds = 1, days = 7)
        }
    }
    buildTypesOrder = arrayListOf(BuildNumber, Aggregate, SafeMerge, Nightly, NightlyCritical, IdePluginsAggregate, KotlinNativeSanityRemoteRunComposite, KotlinxLibrariesCompilation, CompilerDistAndMavenArtifacts, CompilerArtifacts, CompilerArtifactsInDocker, MavenArtifactsDocker, MavenArtifactsAgent, CompilerDistAndMavenArtifactsForIde, BuildGradleIntegrationTests, LibraryReferenceLegacyDocs, LibraryReferenceLatestDocs, Artifacts, ComposeArtifacts, ResolveDependencies, CompilerDist, CompilerDistLocal, CompilerDistLocalOverrideObsoleteJdk, CompilerDistLocalK2, CompileAllClasses, ValidateIdePluginDependencies)

    subProject(UserProjectCompiling.Project)
    subProject(Tests_Windows.Project)
    subProject(Deploy.Project)
    subProject(Aligners.Project)
    subProject(KotlinNative.Project)
    subProject(UserProjectsK2.Project)
    subProject(KotlinBuildTests.Project)
    subProject(Tests_Linux.Project)
    subProject(Service.Project)
    subProject(Tests_MacOS.Project)
})
