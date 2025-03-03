package org.tera201.vcstoolkit.utils

import org.eclipse.uml2.uml.*
import org.tera201.code2uml.uml.util.nl
import org.tera201.umlgraph.graph.Graph
import org.tera201.umlgraph.graph.Vertex
import org.tera201.umlgraph.graphview.arrows.ArrowTypes
import org.tera201.umlgraph.graphview.vertices.elements.ElementTypes

fun Package.toGraph(graph: Graph<String, String>) {
    val root = graph.getOrCreateVertex(name, ElementTypes.PACKAGE)
    packagedElements
        .filter { !it.hasKeyword("unknown") }
        .forEach {
            when (it) {
                is Class -> it.generateClass(graph, root)
                is Interface -> it.generateInterface(graph, root)
                is Enumeration -> it.generateEnumeration(graph, root)
                is Package -> it.generatePackage(graph, root)
            }
        }
}


private fun Package.generatePackage(graph: Graph<String, String>, parent: Vertex<String>) {
    val root = graph.getOrCreateVertex(name, ElementTypes.PACKAGE)

    packagedElements
        .filter { !it.hasKeyword("unknown") }
        .forEach {
            when (it) {
                is Class -> it.generateClass(graph, root)
                is Interface -> it.generateInterface(graph, root)
                is Enumeration -> it.generateEnumeration(graph, root)
                is Package -> it.generatePackage(graph, root)
            }
        }
}

private fun Class.generateClass(graph: Graph<String, String>, parent: Vertex<String>) {
    val root = graph.getOrCreateVertex(name, ElementTypes.CLASS)
    graph.getOrCreateEdge(parent, root, "$name-${parent}", ArrowTypes.DEPENDENCY)
}

private val newLine: CharSequence = "\n"

private val VisibilityKind.asJava
    get() = if (this == VisibilityKind.PACKAGE_LITERAL) "" else "$literal "

private val NamedElement.javaName: String
    get() {
        val longName = qualifiedName.replace("::", ".")
        val k = longName.indexOf('.')
        return longName.substring(k + 1)
    }

private val Classifier.packageAsJava
    get() = "package ${nearestPackage.javaName};$nl"

private val Classifier.importsAsJava
    get() = importedMembers
        .map { "import ${it.javaName};" }
        .filter { !it.startsWith("import java.lang") }
        .joinToString(newLine)

private val Classifier.parentsAsJava: String
    get() {
        val parents = generalizations
            .map { it.general }
            .filter { !it.javaName.endsWith("java.lang.Object") }
            .joinToString { it.name }
        return if (parents.isNotEmpty()) " extends $parents" else ""
    }

private val Class.interfacesAsJava: String
    get() {
        val implemented = interfaceRealizations.joinToString { it.contract.name }
        return if (implemented.isNotEmpty()) " implements $implemented" else ""
    }

private val Property.propertyAsJava
    get() = "$modifiers${type.name} $name;"

private val Property.modifiers: String
    get() {
        var modifiers = visibility.asJava
        if (isStatic) modifiers += "static "
        return modifiers
    }

private val Operation.operationAsJava: String
    get() {
        val returns = returnResult?.type?.name ?: "void"
        val tail = if (isAbstract) ";" else " {$newLine}$newLine"

        return "$modifiers$returns $name$parameters$tail"
    }

private val Operation.modifiers: String
    get() {
        var modifiers = visibility.asJava
        if (isStatic) modifiers += "static "
        if (isAbstract) modifiers += "abstract "
        return modifiers
    }

private val Operation.parameters
    get() = ownedParameters
        .filter { it.direction != ParameterDirectionKind.RETURN_LITERAL }
        .joinToString(prefix = "(", postfix = ")")
        { "${it.type.name} ${it.name}" }

private val Enumeration.modifiers: String
    get() {
        var modifiers = visibility.asJava
        if (isLeaf) modifiers += "final "
        return modifiers
    }

private fun Enumeration.generateEnumeration(graph: Graph<String, String>, parent: Vertex<String>) {
    val root = graph.getOrCreateVertex(name, ElementTypes.ENUM)
    graph.getOrCreateEdge(parent, root, "$name-$parent", ArrowTypes.DEPENDENCY)
}

private val Interface.modifiers: String
    get() {
        var modifiers = visibility.asJava
        if (isLeaf) modifiers += "final "
        return modifiers
    }

private fun Interface.generateInterface(graph: Graph<String, String>, parent: Vertex<String>) {
    val root = graph.getOrCreateVertex(name, ElementTypes.INTERFACE)
    graph.getOrCreateEdge(parent, root, "$name-$parent", ArrowTypes.DEPENDENCY)
}