rootProject.name = "sample"

val api = "${rootProject.name}-api"
val core = "${rootProject.name}-core"
val debug = "${rootProject.name}-debug"

include(api, core, debug)

// load nms
file(core).listFiles()?.filter {
    it.isDirectory && it.name.startsWith("v")
}?.forEach { file ->
    include(":$core:${file.name}")
}
