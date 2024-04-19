package org.tera201.vcstoolkit.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class CollapseTabAction(private val actionManager: ActionManager) : DumbAwareAction(Supplier {"Collapse tab"}, AllIcons.General.CollapseComponent) {

    override fun actionPerformed(event: AnActionEvent) {
        val selectedIndex = actionManager.jtp.selectedIndex
        val selectedTabTitle = actionManager.jtp.getTitleAt(selectedIndex)

        event.project?.let {
            val fullScreenTabInfo = actionManager.openedFxTabs[selectedTabTitle]
            if (fullScreenTabInfo != null) {
                FileEditorManager.getInstance(it).closeFile(fullScreenTabInfo.virtualFile)
                actionManager.setToolBarWithExpand()
            }
        }
    }
}