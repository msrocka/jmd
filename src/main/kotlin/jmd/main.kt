package jmd

import com.github.javaparser.JavaParser
import java.io.File

private data class Config(
    var dir: File
)

fun errout(message: String) {
    System.err.println(message)
}

private fun readConfig(args: Array<String>): Config {
    val conf = Config(File("."))
    if (args.isEmpty())
        return conf
    var lastFlag: String? = null
    for (arg in args) {
        if (arg.startsWith("--")) {
            lastFlag = arg
            continue
        }
        if (lastFlag == null)
            continue
        when (lastFlag) {
            "--dir" -> conf.dir = File(arg)
        }
    }
    return conf
}

private fun parseFolder(dir: File, model: Model) {
    dir.listFiles().forEach { file ->
        if (file.isDirectory) {
            parseFolder(file, model)
        } else if (file.name.endsWith(".java")) {
            val cu = JavaParser.parse(file)
            model.addCU(cu)
        }
    }
}

fun main(args: Array<String>) {
    val config = readConfig(args)
    if (!config.dir.exists() || !config.dir.isDirectory) {
        errout("ERROR: the ${config.dir} is not a folder")
        return
    }
    val model = Model()
    parseFolder(config.dir, model)
    println(model.markdown())
}