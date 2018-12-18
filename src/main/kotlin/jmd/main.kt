package jmd

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File

fun main(args: Array<String>) {
    val path =
        "C:\\Users\\Besitzer\\Projects\\git_repos\\olca-modules\\olca-core\\src\\main\\java\\org\\openlca\\core\\results"
    val dir = File(path)
    val model = Model()
    dir.listFiles().forEach { file ->
        if (file.name.endsWith(".java")) {
            val cu = JavaParser.parse(file)
            model.addCU(cu)
        }
    }
    println(model.markdown())
}