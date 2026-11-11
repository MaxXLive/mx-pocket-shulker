tasks.register("build") {
    group = "build"
    description = "Builds the Fabric client mod and the Paper plugin."
    dependsOn(gradle.includedBuild("client-fabric").task(":build"))
    dependsOn(gradle.includedBuild("server-paper").task(":build"))
}

tasks.register("clean") {
    group = "build"
    dependsOn(gradle.includedBuild("client-fabric").task(":clean"))
    dependsOn(gradle.includedBuild("server-paper").task(":clean"))
}
