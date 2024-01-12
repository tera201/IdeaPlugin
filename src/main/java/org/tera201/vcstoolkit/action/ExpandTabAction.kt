package org.tera201.vcstoolkit.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.impl.JComponentEditorProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.containers.stream
import javafx.embed.swing.JFXPanel
import org.tera201.vcstoolkit.helpers.FullScreenTabInfo
import java.util.function.Supplier
import javax.swing.JComponent

class ExpandTabAction(private val actionManager: ActionManager) : DumbAwareAction(Supplier {"Plugin Settings"}, AllIcons.General.ExpandComponentHover) {
    override fun actionPerformed(event: AnActionEvent) {
        val selectedIndex = actionManager.jtp.selectedIndex
        val selectedTabTitle = actionManager.jtp.getTitleAt(selectedIndex)
        val content = actionManager.jtp.selectedComponent

        event.project?.let {
            val jfxPanel = (content as JComponent).components.stream().filter { it is JFXPanel }.findAny().get() as JFXPanel
            val editor = JComponentEditorProvider.openEditor(
                it, selectedTabTitle,
                jfxPanel as JComponent
            )
            actionManager.openedFxTabs.set(selectedTabTitle, FullScreenTabInfo(selectedIndex, jfxPanel, editor[0].file))
            actionManager.setToolBarWithCollapse()
        }
    }
}