rootProject.name = "PocketShulker"

// Two independent builds: Fabric Loom and paperweight don't share a build well, so each keeps its own setup.
// Both compile the shared opening logic from common/.
includeBuild("client-fabric")
includeBuild("server-paper")
