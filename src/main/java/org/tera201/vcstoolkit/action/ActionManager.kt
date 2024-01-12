package org.tera201.vcstoolkit.action

import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBTabbedPane
import org.tera201.vcstoolkit.helpers.FullScreenTabInfo

class ActionManager(val jtp: JBTabbedPane, private val toolWindow: ToolWindow) {
    val openedFxTabs = HashMap<String, FullScreenTabInfo>()
    private val expandTabAction = ExpandTabAction(this)
    private val collapseTabAction = CollapseTabAction(this)
    private val showSettingsAction = ShowSettingsAction()

    private val buttonsFxWithExpand = listOf(expandTabAction, showSettingsAction)
    private val buttonsFxWithCollapse = listOf(collapseTabAction, showSettingsAction)
    private val settingButton = listOf(showSettingsAction)

    fun setDefaultToolBar() {
        toolWindow.setTitleActions(settingButton)
    }

    fun setToolBarWithExpand() {
        toolWindow.setTitleActions(buttonsFxWithExpand)
    }

    fun setToolBarWithCollapse() {
        toolWindow.setTitleActions(buttonsFxWithCollapse)
    }
}