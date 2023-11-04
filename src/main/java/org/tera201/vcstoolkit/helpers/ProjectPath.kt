package org.tera201.vcstoolkit.helpers

class ProjectPath {
    var isExternal:Boolean
    var path:String
    var copyPath:String
    constructor() {
        isExternal = false
        path = ""
        copyPath = ""
    }

    constructor(isExternal:Boolean, path: String, copyPath: String) {
        this.isExternal = isExternal
        this.path = path
        this.copyPath = copyPath
    }
}