package org.tera201.vcstoolkit.utils

import org.tera201.vcstoolkit.tabs.FXCircleTab
import org.eclipse.uml2.uml.*
import org.tera201.elements.circle.ClassCircle
import org.tera201.elements.circle.PackageCircle

private const val height = 500.0
private const val gap = 8000.0
fun Package.toCircle(number: Int=0) {
    val packageCircle = PackageCircle(name, 6000.0, 5500.0, height)
    packageCircle.translateY = number * gap
    FXCircleTab.circleSpace.add(packageCircle)
    packagedElements
        .filter { !it.hasKeyword("unknown") }
        .forEach {
            when (it) {
                is Package -> it.generatePackage(packageCircle)
                is Class -> it.generateClass(packageCircle)
            }
        }
}

private fun Package.generatePackage(circleParent: PackageCircle) {
    val size = if (ownedComments.isEmpty()) 700 else ownedComments[0].body.toDouble()
    val packageCircle = PackageCircle(name, circleParent.innerRadius/2 + 500, circleParent.innerRadius/2, height)
    circleParent.addObject(packageCircle)
    packagedElements
        .filter { !it.hasKeyword("unknown") }
        .forEach {
            when (it) {
                is Package -> it.generatePackage(packageCircle)
                is Class -> it.generateClass(packageCircle)
                is Interface -> it.generateInterface(packageCircle)
                is Enumeration -> it.generateEnumeration(packageCircle)
            }
        }
}

private fun Class.generateClass(circleParent: PackageCircle) {
    val size = if (ownedComments.isEmpty()) 700.0 else ownedComments[0].body.toDouble()
    val methods = if (ownedComments.size > 1) (ownedComments[1].body.toDouble() + 1) * 10 else 10.0
    val side =  size / 2
    val classCircle = ClassCircle(name, side + methods * 10, side, height)
    circleParent.addObject(classCircle)
}

private fun Interface.generateInterface(circleParent: PackageCircle) {
    val size = if (ownedComments.isEmpty()) 700.0 else ownedComments[0].body.toDouble()
    val methods = if (ownedComments.size > 1) (ownedComments[1].body.toDouble() + 1) * 10 else 10.0
    val side = size / 2
    val classCircle = ClassCircle(name, side + methods * 10, side, height)
    circleParent.addObject(classCircle)
}

private fun Enumeration.generateEnumeration(circleParent: PackageCircle) {
    val classCircle = ClassCircle(name, 1000.0, 700.0, height)
    circleParent.addObject(classCircle)
}

private val Classifier.parentsAsJava: String
    get() {
        val parents = generalizations
            .map { it.general }
            .filter { !it.javaName.endsWith("java.lang.Object") }
            .joinToString { it.name }
        return if (parents.isNotEmpty()) " extends $parents" else ""
    }


private val NamedElement.javaName: String
    get() {
        val longName = qualifiedName.replace("::", ".")
        val k = longName.indexOf('.')
        return longName.substring(k + 1)
    }