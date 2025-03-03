package org.tera201.vcstoolkit.utils

import org.tera201.code2uml.util.messages.DataBaseUtil
import org.tera201.code2uml.util.messages.getModel
import org.tera201.code2uml.util.messages.getPackage
import org.tera201.code2uml.util.messages.getRootPackageIds
import org.tera201.umlgraph.graph.Graph
import org.tera201.umlgraph.graph.Vertex
import org.tera201.umlgraph.graphview.arrows.ArrowTypes
import org.tera201.umlgraph.graphview.vertices.elements.ElementTypes

fun toPackage(graph: Graph<Int, String>, modelId:Int, dataBaseUtil: DataBaseUtil) {
    val node = getVertexOrCreate(graph, 0, dataBaseUtil.getModel(modelId).name, ElementTypes.PACKAGE)
    dataBaseUtil.getRootPackageIds(modelId).forEach { generatePackage(graph, node, dataBaseUtil, it, modelId) }
}

private fun generatePackage(graph: Graph<Int, String>, parent: Vertex<Int>?, dataBaseUtil: DataBaseUtil, id: Int, modelId:Int, prefixName: String = "") {
    val packageDB = dataBaseUtil.getPackage(id, modelId)
//    val name = if (prefixName.length > 0) "$prefixName.${packageDB.name}" else packageDB.name
//    if (packageDB.childrenId.size == 1) {
//        packageDB.childrenId.forEach { generatePackage(graph, null, dataBaseUtil, it, modelId, name) }
//        return
//    }
    val node = getVertexOrCreate(graph, id, packageDB.name, ElementTypes.PACKAGE)
//    val node = graph.insertVertex(packageDB.name, ElementTypes.PACKAGE)
    parent?.let {  graph.getOrCreateEdge(parent, node, "${node.label}-${parent.label}", ArrowTypes.DEPENDENCY) }
    packageDB.childrenId.forEach { generatePackage(graph, node, dataBaseUtil, it, modelId) }
}

private fun getVertexOrCreate(graph: Graph<Int, String>, nodeId:Int, nodeName: String, types: ElementTypes): Vertex<Int> {
    return graph.getOrCreateVertex(nodeId, types, nodeName)
}