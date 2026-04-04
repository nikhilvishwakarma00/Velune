import java.io.File

fun main() {
    val dir = File("c:/Users/Pooja Vishwakarma/Documents/velune/app/src/main/kotlin/com/nikhil/yt/ui")
    if (!dir.exists()) {
        println("Directory not found")
        return
    }
    
    var fileCount = 0
    var occurenceCount = 0
    
    dir.walk().filter { it.extension == "kt" }.forEach { file ->
        val text = file.readText()
        // Very basic approach: split by IconButton
        val blocks = text.split("IconButton")
        var fileHit = false
        for (i in 1 until blocks.size) {
            val block = blocks[i]
            // check the next 500 characters
            val snippet = block.take(500)
            if (snippet.contains("Icon") && snippet.contains("contentDescription = null")) {
                occurenceCount++
                fileHit = true
            }
        }
        if (fileHit) {
            fileCount++
            println(file.name)
        }
    }
    
    println("Total files affected: $fileCount")
    println("Total occurences: $occurenceCount")
}
