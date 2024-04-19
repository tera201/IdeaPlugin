package org.tera201.vcstoolkit.tabs

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import org.tera201.vcstoolkit.helpers.SharedModel
import java.awt.Dimension
import java.util.HashMap
import javax.swing.JPanel

class TabManager (val project:Project) {
    private val jtp = JBTabbedPane()
    private val modelListContent = SharedModel()
    private val tabMap = hashMapOf(TabEnum.GRAPH to FXGraphTab(this, modelListContent), TabEnum.CITY to FXCityTab(this, modelListContent),
        TabEnum.CIRCLE to  FXCircleTab(this), TabEnum.GIT to GitTab(this, modelListContent))
    init {
        jtp.autoscrolls = true
        jtp.add(TabEnum.GRAPH.value, tabMap[TabEnum.GRAPH])
        jtp.add(TabEnum.CITY.value, tabMap[TabEnum.CITY])
        jtp.add(TabEnum.CIRCLE.value,  tabMap[TabEnum.CIRCLE])
        jtp.add(TabEnum.GIT.value, tabMap[TabEnum.GIT])
        jtp.preferredSize = Dimension(500, 400)
    }

    fun getJBTabbedPane(): JBTabbedPane {
        return jtp
    }

    fun getTabMap() : HashMap<TabEnum, JPanel> {
        return tabMap;
    }

    fun getCurrentProject(): Project {
        return project
    }
}