package org.tera201.vcstoolkit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.tera201.vcstoolkit.action.ActionManager
import org.tera201.vcstoolkit.tabs.*
import javax.swing.SwingUtilities
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener


class VCSToolWindowFactoryKt : ToolWindowFactory {

    private val log: Logger = LogManager.getLogger(VCSToolWindowFactoryKt::class.java)

    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool windowA
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        SwingUtilities.invokeLater {
            val tabManager = TabManager(project)
            val jtp = tabManager.getJBTabbedPane()
            val actionManager = ActionManager(jtp, toolWindow)

            jtp.addChangeListener(object : ChangeListener {
                override fun stateChanged(e: ChangeEvent?) {
                    val selectedTab = jtp.getTitleAt(jtp.selectedIndex)
                    if ((selectedTab == TabEnum.CITY.value || selectedTab == TabEnum.CIRCLE.value) &&
                        actionManager.openedFxTabs[selectedTab] == null)
                        actionManager.setToolBarWithExpand()
                    else if (actionManager.openedFxTabs[selectedTab] != null)
                        actionManager.setToolBarWithCollapse()
                    else
                        actionManager.setDefaultToolBar()
                }

            })

            ApplicationManager.getApplication().messageBus.connect()
                .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                        if (file.extension == null && (file.name == TabEnum.CITY.value || file.name == TabEnum.CIRCLE.value)) {
                            val fullScreenTabInfo = actionManager.openedFxTabs[file.name]
                        if (fullScreenTabInfo != null) {
                                val fxTab = jtp.getComponentAt(fullScreenTabInfo.index) as FXTab
                                fxTab.setJFXPanel(fullScreenTabInfo.jfxPanel)
                                actionManager.openedFxTabs.remove(file.name)
                            }
                        }
                    }
                })


            val content = contentFactory.createContent(jtp, "", false)
            toolWindow.contentManager.addContent(content)
        }
    }
}