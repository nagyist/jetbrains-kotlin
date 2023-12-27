package UserProjectsK2

import UserProjectsK2.buildTypes.*
import UserProjectsK2.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudImage
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudProfile
import jetbrains.buildServer.configs.kotlin.kubernetesCloudImage
import jetbrains.buildServer.configs.kotlin.kubernetesCloudProfile

object Project : Project({
    id("UserProjectsK2")
    name = "K2 User Projects"
    description = "User projects with K2 kotlin compiler"

    vcsRoot(FlystoK2VCS)
    vcsRoot(KotlinxCoroutinesK2VCS)
    vcsRoot(ReaktiveMppK2VCS)
    vcsRoot(KotlinxDateTimeK2VCS)
    vcsRoot(SpaceK2VCS)
    vcsRoot(ArrowVCS)
    vcsRoot(ExposedK2VCS)
    vcsRoot(KmmSampleK2VCS)
    vcsRoot(SphinxK2VCS)
    vcsRoot(KspK2VCS)
    vcsRoot(KotlinPoetK2VCS)
    vcsRoot(IntellijK2VCS)
    vcsRoot(RealmKotlinK2VCS)
    vcsRoot(FirebaseK2VCS)
    vcsRoot(NowinandroidVCS)
    vcsRoot(KtorForK2VCS)
    vcsRoot(KmShopK2VCS)
    vcsRoot(MultiplatformSettingsK2VCS)
    vcsRoot(SmithyK2VCS)
    vcsRoot(KaMPKitK2VCS)
    vcsRoot(PeopleInSpaceK2VCS)
    vcsRoot(KotlinxAtomicFuK2VCS)
    vcsRoot(DokkaK2VCS)
    vcsRoot(lcarswmAppVCS)
    vcsRoot(SpringPetclinicK2VCS)
    vcsRoot(AwsSdkSmithyK2VCS)
    vcsRoot(TrustWalletK2VCS)
    vcsRoot(KotlinxBenchmarkK2VCS)
    vcsRoot(MokoResourcesK2VCS)
    vcsRoot(AnkiAndroidVCS)

    buildType(TrustWalletK2)
    buildType(AwsSdkSmithyK2)
    buildType(ExposedK2)
    buildType(ArrowK2)
    buildType(MultiplatformSettingsK2)
    buildType(KtorK2)
    buildType(SpaceiOSK2)
    buildType(KspPlaygroundK2)
    buildType(LcarswmAppK2)
    buildType(FirebaseK2)
    buildType(KotlinxDateTimeK2)
    buildType(IntellijK2)
    buildType(KotlinPoetK2)
    buildType(SpaceK2)
    buildType(KotlinxBenchmarkK2)
    buildType(UserProjectsAggregateK2)
    buildType(KmShopK2)
    buildType(FlystoK2)
    buildType(PeopleInSpaceK2)
    buildType(RealmKotlinK2)
    buildType(KotlinxAtomicFuK2)
    buildType(NowinandroidK2)
    buildType(KaMPKitK2)
    buildType(DokkaK2)
    buildType(SpringPetclinicK2)
    buildType(KmmSampleK2)
    buildType(SphinxK2)
    buildType(MokoResourcesK2)
    buildType(KotlinxCoroutinesK2)
    buildType(AnkiAndroidK2)
    buildType(ReaktiveMppK2)

    features {
        kubernetesCloudImage {
            id = "PROJECT_EXT_1"
            profileId = "titan-up-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "titan-up-k8s-up-macos-4cpu-8gb"
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
                          image: mac-registry.eqx.k8s.intellij.net/kotlin-userprojects/userprojects-arm64-agent:xcode-15.1
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
        kubernetesCloudImage {
            id = "PROJECT_EXT_2"
            profileId = "titan-up-k8s"
            agentPoolId = "-2"
            agentNamePrefix = "titan-up-k8s-up-macos-8cpu-16gb"
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
                          image: mac-registry.eqx.k8s.intellij.net/kotlin-userprojects/userprojects-arm64-agent:xcode-15.1
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
            id = "titan-up-aws"
            name = "titan-up-aws"
            terminateAfterBuild = true
            terminateIdleMinutes = 10
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            authType = accessKey {
                keyId = "credentialsJSON:6176dae8-b17c-41a3-bb98-2fbd865083bc"
                secretKey = "credentialsJSON:1be3bac3-69f9-49ee-9465-a46431abbdd1"
            }
        }
        amazonEC2CloudImage {
            id = "titan-up-aws-linux-4cpu-16gb"
            profileId = "titan-up-aws"
            agentPoolId = "-2"
            name = "titan-up-aws-linux-4cpu-16gb"
            maxInstancesCount = 50
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
                          "Value": "titan-up-aws-linux-4cpu-16gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-up-aws"
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
            id = "titan-up-aws-linux-8cpu-32gb"
            profileId = "titan-up-aws"
            agentPoolId = "-2"
            name = "titan-up-aws-linux-8cpu-32gb"
            maxInstancesCount = 50
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
                          "Value": "titan-up-aws-linux-8cpu-32gb"
                        },
                        {
                          "Key": "CloudProfileId",
                          "Value": "titan-up-aws"
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
            id = "titan-up-k8s"
            name = "titan-up-k8s"
            terminateAfterBuild = true
            terminateIdleMinutes = 30
            apiServerURL = "https://eqx.k8s.intellij.net:6443"
            caCertData = "credentialsJSON:d0854a8d-f28f-4fa4-945a-27fa9c2d4984"
            namespace = "agents-kotlin-userprojects"
            authStrategy = token {
                token = "credentialsJSON:6c6d074d-427d-42b9-b9ae-fb045fa07662"
            }
        }
    }
    buildTypesOrder = arrayListOf(UserProjectsAggregateK2)

    subProject(UserProjectsK2Rebase.Project)
})
