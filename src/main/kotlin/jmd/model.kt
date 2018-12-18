package jmd

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.Comment
import java.util.*


fun formatComment(comment: Optional<Comment>): String {
    if (!comment.isPresent)
        return "_undocumented_\n"
    val c = comment.get()
    if (!c.isJavadocComment)
        return "_undocumented_\n"
    var s = ""
    c.content.lines().forEach { line ->
        var l = line.trim()
        if (l.startsWith("/**")) {
            l = l.substring(3)
        }
        if (l.endsWith("*/")) {
            l = l.substring(0, l.length - 2)
        }
        if (l.startsWith("* ")) {
            l = l.substring(2)
        }
        l = l.trim()
        if (!l.isEmpty()) {
            if (l == "*") {
                l = ""
            }
            s += "$l\n"
        }
    }
    return s
}

class Model {

    val packages = mutableMapOf<String, Package>()

    fun getPackage(name: String): Package {
        var p = packages[name]
        if (p != null)
            return p
        p = Package(name)
        packages[name] = p
        return p
    }

    fun addCU(cu: CompilationUnit) {
        val pd = cu.packageDeclaration
        val pack = if (pd.isPresent) {
            getPackage(pd.get().nameAsString)
        } else {
            getPackage("default")
        }
        cu.types.forEach { t ->
            if (t.isPublic) {
                pack.addType(t)
            }
        }
    }

    fun markdown(): String {
        var s = ""
        val packs = packages.values.sortedBy { p ->
            p.name
        }
        packs.forEach { p ->
            s += p.markdown()
        }
        return s
    }

}

class Package(val name: String) {

    val types = mutableListOf<Type>()

    fun addType(t: TypeDeclaration<*>) {
        types.add(Type(name + "." + t.name, t))
    }

    fun markdown(): String {
        var s = "# Package: $name\n\n"
        types.sortBy { it.name }
        types.forEach {
            s += it.markdown()
        }
        return s
    }
}

class Type(val id: String, t: TypeDeclaration<*>) {

    val name = t.nameAsString
    val doc = formatComment(t.comment)
    val fields = mutableListOf<Field>()
    val methods = mutableListOf<Method>()
    val constructors = mutableListOf<Constructor>()
    var superTypes: String? = null

    init {
        t.fields.forEach { f ->
            if (f.isPublic) {
                fields.add(Field(f))
            }
        }
        val ciDef = if (t.isClassOrInterfaceDeclaration)
            t.asClassOrInterfaceDeclaration()
        else
            null
        t.members.forEach { m ->
            if (m.isMethodDeclaration) {
                val method = m.asMethodDeclaration();
                if (method.isPublic || (ciDef != null && ciDef.isInterface)) {
                    methods.add(Method(method))
                }
            } else if (m.isConstructorDeclaration) {
                val constructor = m.asConstructorDeclaration()
                if (constructor.isPublic) {
                    constructors.add(Constructor(constructor))
                }
            }
        }

        if (ciDef != null) {
            var extends = ""
            ciDef.extendedTypes.forEach { s ->
                if (extends != "") {
                    extends += ", "
                }
                extends += s.nameAsString
            }
            ciDef.implementedTypes.forEach { s ->
                if (extends != "") {
                    extends += ", "
                }
                extends += s.nameAsString
            }
            if (extends != "") {
                superTypes = extends
            }
        }
    }

    fun markdown(): String {
        // var s = "<a name=\"$id\"></a>\n\n"
        var s = "## $name"
        if (superTypes != null) {
            s += " > $superTypes"
        }
        s += "\n\n$doc\n\n"
        constructors.sortBy { it.name }
        constructors.forEach { s += it.markdown() }
        fields.sortBy { it.name }
        fields.forEach { s += it.markdown() }
        methods.sortBy { it.name }
        methods.forEach { s += it.markdown() }
        return s
    }
}

class Field(d: FieldDeclaration) {

    val name = d.variables[0].nameAsString
    val doc = formatComment(d.comment)
    val type = d.variables[0].typeAsString

    fun markdown(): String {
        var s = "### $name : $type\n\n"
        s += "$doc\n\n"
        return s
    }
}

class Method(d: MethodDeclaration) {

    val name = d.nameAsString
    private var doc = formatComment(d.comment)
    private val type = d.type.toString()
    private val params = mutableListOf<Param>()

    init {
        d.parameters.forEach { p ->
            params.add(Param(p))
        }
        if (d.annotations != null) {
            val a = d.annotations.find { a -> a.nameAsString == "Override" }
            if (a != null && !d.javadocComment.isPresent) {
                doc = "_Overridden method of supertype._"
            }
        }
    }

    fun markdown(): String {
        var s = "### $name() : $type\n\n"
        if (!params.isEmpty()) {
            s += "Parameters:\n\n"
            params.forEach { p ->
                s += "* ${p.name} : ${p.type}\n"
            }
            s += "\n"
        }
        s += "$doc\n\n"
        return s
    }
}

class Constructor(d: ConstructorDeclaration) {

    val name = d.nameAsString
    val doc = formatComment(d.comment)
    val params = mutableListOf<Param>()

    init {
        d.parameters.forEach { p ->
            params.add(Param(p))
        }
    }

    fun markdown(): String {
        var s = "### $name()\n\n"
        if (!params.isEmpty()) {
            s += "Parameters:\n\n"
            params.forEach { p ->
                s += "* ${p.name} : ${p.type}\n"
            }
            s += "\n"
        }
        s += "$doc\n\n"
        return s
    }
}

class Param(d: Parameter) {
    val name = d.name
    val type = d.type.toString()
}