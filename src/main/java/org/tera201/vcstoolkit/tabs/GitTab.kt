package org.tera201.vcstoolkit.tabs

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColorUtil
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.UIUtil
import javafx.application.Platform
import model.console.BuildModel
import org.eclipse.uml2.uml.Model
import org.repodriller.scm.SCMRepository
import org.tera201.code2uml.java20.console.JavaParserRunner
import org.tera201.code2uml.uml.util.UMLModelHandler
import org.tera201.vcstoolkit.helpers.ProjectPath
import org.tera201.vcstoolkit.helpers.SharedModel
import org.tera201.vcstoolkit.services.VCSToolkitCache
import org.tera201.vcstoolkit.services.settings.VCSToolkitSettings
import org.tera201.vcstoolkit.utils.toCircle
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.*
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlin.concurrent.thread

class GitPanel : JPanel() {
    private var settings: VCSToolkitSettings = VCSToolkitSettings.getInstance()
    private var cache: VCSToolkitCache = VCSToolkitCache.getInstance()
    private var myRepo: SCMRepository? = null
    private val getButton = JButton("Get")
    private val urlField = JTextField()
    private val analyzeButton = JButton("Analyze")
    private val saveUmlFileButton = JButton("Save UML model")
    private val getUmlFileButton = JButton("Get UML model")
    private val buildModel = BuildModel()
    private val logsJTextArea = JTextArea()
    val logsJBScrollPane = JBScrollPane(
        logsJTextArea,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
    companion object {
        val models = ArrayList<Model>()
        val modelListContent = SharedModel()
    }
    private val handler = UMLModelHandler()
    private var isClearingSelection = false
    private val branchListModel = DefaultListModel<String>()
    private val tagListModel = DefaultListModel<String>()
    private val branchList = JBList(branchListModel)
    private val tagList = JBList(tagListModel)
    val modelList = JBList(modelListContent)
    private val branchListScrollPane = JBScrollPane(branchList)
    private val tagListScrollPane = JBScrollPane(tagList)
    private val modelListScrollPane = JBScrollPane(modelList)
    val pathJTree = Tree()
    private val projectComboBox = ComboBox<String>()
    private val openProjectButton = JButton("Open project")
    private val vcSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, branchListScrollPane, tagListScrollPane)
    private val filesTreeJBScrollPane =
        JBScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
    private val showSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filesTreeJBScrollPane, vcSplitPane)
    private val logModelSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logsJBScrollPane, modelListScrollPane)
    private val projectCache = "${System.getProperty("user.dir")}/VCSToolkitCache/"
    private val modelCache = "${System.getProperty("user.dir")}/VCSToolkitCache/Models"
    
    init {
        initGit()
    }

    private fun initGit() {

        try {
            File(projectCache).mkdirs()
            File(modelCache).mkdirs()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        this.layout = FlowLayout(FlowLayout.LEFT)
        logsJBScrollPane.isVisible = settings.showGitLogs

        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(VCSToolkitSettings.SettingsChangedListener.TOPIC, object :
                VCSToolkitSettings.SettingsChangedListener {
                override fun onSettingsChange(settings: VCSToolkitSettings) {
                    logsJBScrollPane.isVisible = settings.showGitLogs
                    if (settings.externalProjectMode == 1 &&  cache.projectPathMap[projectComboBox.selectedItem]!!.isExternal) {
                        branchListModel.clear()
                        tagListModel.clear()
                    } else {
                        populateJBList(branchListModel, buildModel.getBranches(myRepo).filter { it != "HEAD" })
                        populateJBList(tagListModel, buildModel.getTags(myRepo))
                    }
                }
            })

        urlField.text = "https://github.com/arnohaase/a-foundation.git"
        this.minimumSize = Dimension(0, 200)
        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                super.componentResized(e)
                val newWidth = e!!.component.width
                val newHeight = e!!.component.height
                urlField.preferredSize = (Dimension(newWidth - getButton.width - 40, getButton.height))
                showSplitPane.preferredSize =
                    Dimension(newWidth - 20, ((newHeight - 130 ) * 2f / 3f).toInt())
                logModelSplitPane.preferredSize = Dimension(
                    newWidth - 140,
                    ((newHeight - 130 ) * 1f / 3f).toInt()
                )

            }
        })

        getButton.addActionListener {
            thread {
                //TODO: add regex
                if (urlField.text != "") {
                    getRepoByUrl(urlField.text)
                }
            }
        }
        setupTree()
        configureSplitPanes()
        this.add(urlField)
        this.add(getButton)
        this.add(showSplitPane)
        addLogPanelButtons(this)
        this.add(logModelSplitPane)
        addModelControlPanel(this)
        addProjectPane()
    }

    private fun addProjectPane() {
        cache.projectPathMap["Current project"] = ProjectPath(true, ProjectManager.getInstance().openProjects[0].basePath.toString(), "$projectCache/${ProjectManager.getInstance().openProjects[0].name}")

        if (cache.projectPathMap.isNotEmpty()) {
            cache.projectPathMap.keys.forEach {
                projectComboBox.addItem(it)
            }
            if (cache.lastProject != "") {
                projectComboBox.selectedItem = cache.lastProject
                cache.projectPathMap[cache.lastProject]?.let { updatePathPanelAndGitLists(cache.lastProject, it.path) }
            }
        }

        File(projectCache).list { dir, name -> File(dir, name).isDirectory.and(name != "Models") }?.forEach {
            val path = "$projectCache/$it"
            if ((0 until projectComboBox.model.size).none { i -> projectComboBox.model.getElementAt(i) == it }) {
                cache.projectPathMap[it] = ProjectPath(false, path, path)
                projectComboBox.addItem(it)
            }
        }

        projectComboBox.addActionListener {
            val selectedProject = projectComboBox.selectedItem as String
            val projectPath = cache.projectPathMap[selectedProject]

            val directory = File(projectPath?.path)
            if (directory.exists() && directory.isDirectory) {
                projectPath?.let { updatePathPanelAndGitLists(selectedProject, it.path) }
            } else {
                projectComboBox.removeItem(selectedProject)
                cache.projectPathMap.remove(selectedProject)
                projectComboBox.selectedItem = cache.lastProject
                val group = NotificationGroupManager.getInstance().getNotificationGroup("VCSToolkitNotify")
                val content = "The project with the specified path was not found."
                val notification: Notification =
                    group.createNotification("Open error", content, NotificationType.ERROR)
                Notifications.Bus.notify(notification, null);
            }
            cache.lastProject = selectedProject
        }

        openProjectButton.addActionListener {
            val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            val toSelect = if (projectCache.isEmpty()) null else LocalFileSystem.getInstance()
                .findFileByPath(projectCache)
            val selectedDirectory = FileChooser.chooseFile(descriptor, null, toSelect)
            if (selectedDirectory != null) {
                val newProjectName = selectedDirectory.name
                val copyPath = "$projectCache/${selectedDirectory.name}"
                cache.projectPathMap[newProjectName] = ProjectPath(true, selectedDirectory.path, copyPath)
                projectComboBox.addItem(newProjectName)
                projectComboBox.selectedItem = newProjectName
            }
        }

        this.add(projectComboBox)
        this.add(openProjectButton)
    }

    private fun externalWarningNotification() {
        val group = NotificationGroupManager.getInstance().getNotificationGroup("VCSToolkitNotify")
        val content = "VCS for external projects was disabled. Please use the built-in VCS in the IDE manually."
        val notification: Notification =
            group.createNotification("VCS warning", content, NotificationType.WARNING)
        Notifications.Bus.notify(notification, null);
    }

    private fun updatePathPanelAndGitLists(projectName: String, projectPath: String) {
        thread {
            val isRepo = File("$projectPath/.git").exists()
            if (myRepo?.repoName != projectName && isRepo) myRepo = buildModel.getRepository(projectPath)
            updatePathPanel()
            if (isRepo && !(cache.projectPathMap[projectName]!!.isExternal && settings.externalProjectMode == 1)) {
                populateJBList(branchListModel, buildModel.getBranches(myRepo).filter { it != "HEAD" })
                populateJBList(tagListModel, buildModel.getTags(myRepo))
            } else if (cache.projectPathMap[projectName]!!.isExternal && settings.externalProjectMode == 1) {
                externalWarningNotification()
            } else {
                val group = NotificationGroupManager.getInstance().getNotificationGroup("VCSToolkitNotify")
                val content = "The project doesn't have git repo."
                val notification: Notification =
                    group.createNotification("Opps", content, NotificationType.WARNING)
                Notifications.Bus.notify(notification, null);
                branchListModel.clear()
                tagListModel.clear()
            }
        }
    }

    private fun configureSplitPanes() {
        showSplitPane.setUI(CustomSplitPaneUI())
        vcSplitPane.setUI(CustomSplitPaneUI())
        logModelSplitPane.setUI(CustomSplitPaneUI())
        resizeSplitPaneDivider(showSplitPane)
        resizeSplitPaneDivider(vcSplitPane)
        resizeSplitPaneDivider(logModelSplitPane)
        setupListSelectionListeners()
        setupListDoubleClickAction()
    }

    private fun resizeSplitPaneDivider(jSplitPane: JSplitPane, ) {
        jSplitPane.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                super.componentResized(e)
                if (jSplitPane.orientation == JSplitPane.HORIZONTAL_SPLIT) {
                    val newWidth = e!!.component.width
                    if (jSplitPane.dividerLocation != newWidth / 2)
                        jSplitPane.dividerLocation = newWidth / 2
                } else {
                    val newHeight = e!!.component.height
                    if (jSplitPane.dividerLocation != newHeight / 2)
                        jSplitPane.dividerLocation = newHeight / 2
                }
            }
        })
    }

    private fun setupListSelectionListeners() {
        branchList.addClearSelectorByAnotherList(tagList)
        tagList.addClearSelectorByAnotherList(branchList)
        modelList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_BACK_SPACE) {
                    val selectedIndices = modelList.selectedIndices
                    for (i in selectedIndices.reversedArray()) {
                        modelListContent.removeAt(i)
                        models.removeAt(i)
                    }
                }
            }
        })
    }

    fun JBList<String>.addClearSelectorByAnotherList(anotherList: JBList<String>) {
        this.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if ((e != null) &&!(e.isMetaDown || e.isShiftDown)
                    && (!isClearingSelection && anotherList.selectedValue != null)) {
                    isClearingSelection = true
                    anotherList.clearSelection()
                    isClearingSelection = false
                }
                val selected = this@addClearSelectorByAnotherList.selectedValuesList.size + anotherList.selectedValuesList.size
                if (selected == 1) {
                    analyzeButton.text = "Analyze"
                }
                else {
                    analyzeButton.text = "AnalyzeAll"
                }
            }
        })
    }

    private fun setupListDoubleClickAction() {
        val branchMouseEvent = mouseClickListenerForGitList(branchList)
        val tagMouseEvent = mouseClickListenerForGitList(tagList)
        branchList.addMouseListener(branchMouseEvent)
        tagList.addMouseListener(tagMouseEvent)
    }

    private fun mouseClickListenerForGitList(jbList: JBList<String>):MouseAdapter {
        return object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e!!.clickCount == 2) {
                    val index = jbList.locationToIndex(e.point)
                    if (index >= 0) {
                        val selectedItem = jbList.model.getElementAt(index)
                        onListItemDoubleClicked(selectedItem)
                    }
                }
            }
        }
    }

    private fun onListItemDoubleClicked(item: String) {
        println("Double clicked on item: $item")
        buildModel.checkout(myRepo, item)
        updatePathPanel()
    }

    private fun populateJBList(targetListModel: DefaultListModel<String>, stringList: List<String>) {
        targetListModel.clear()
        if (!cache.projectPathMap[projectComboBox.selectedItem]!!.isExternal || settings.externalProjectMode != 1)
        targetListModel.addAll(stringList)
    }

    private fun addLogPanelButtons(mainJPanel: JPanel) {
        val logButtonsPanel = JPanel()
        logButtonsPanel.layout = BoxLayout(logButtonsPanel, BoxLayout.Y_AXIS)
        val clearLogButton = JButton("Clear log")

        clearLogButton.addActionListener {
            logsJTextArea.text = null
        }
        logButtonsPanel.add(clearLogButton)
        mainJPanel.add(logButtonsPanel)
    }

    private fun addModelControlPanel(mainJPanel: JPanel) {
        val modelControlPanel = JPanel()
        analyzeButton.addActionListener {
            if (myRepo != null) {
                val javaParserRunner = JavaParserRunner()
                thread {
                    val allList = branchList.selectedValuesList + tagList.selectedValuesList
                    models.clear()
                    modelListContent.clear()

                    for (i in allList) {
                        buildModel.checkout(myRepo, i)
                        val javaFiles = javaParserRunner.collectFiles(myRepo!!.path)
                        logsJTextArea.append("Start analyzing $i.\n")
                        if (allList.size > 1) {
                            models.add(javaParserRunner.buildModel(i, javaFiles))
                            saveUmlFileButton.text = "Save UML model pack"
                        }
                        else {
                            models.add(javaParserRunner.buildModel(i, javaFiles, logsJTextArea))
                            saveUmlFileButton.text = "Save UML model"
                        }
                        logsJTextArea.append("End analyzing $i.\n")
                        logsJTextArea.caret.dot = logsJTextArea.text.length
                    }

                    modelListContent.addAll(models.stream().map { it.name }.toList())

                    Platform.runLater {
                        FXCircleTab.circleSpace.clean()
                        for (i in 0 until models.size) {
                            models[i].toCircle(i)
                        }
                        FXCircleTab.circleSpace.mainListObjects.forEach { it.updateView() }
                    }
                }
            }
            else logsJTextArea.append("Get some repo for analyzing.\n")
        }

        getUmlFileButton.addActionListener {
            val descriptor = FileChooserDescriptor(
                true, false,
                false, false, false, false
            );
            descriptor.setTitle("Get UML-Model");
            val toSelect = if (modelCache.isEmpty()) null else LocalFileSystem.getInstance()
                .findFileByPath(modelCache)
            val virtualFile = FileChooser.chooseFile(descriptor, null, toSelect)

            if (virtualFile != null) {
                logsJTextArea.append("Get model from file: ${virtualFile.path}\n")
            }
        }
//
        saveUmlFileButton.addActionListener {
            val title =  if (models.size == 1) "SAVE UML-MODEL" else "SAVE UML-MODEL PACK"
            val fileNameExt = if (models.size == 1) "" else "Pack"
            val descriptor = FileSaverDescriptor(
                title, "Choose the destination file",
                ".json"
            );
            val toSelect = if (modelCache.isEmpty()) null else LocalFileSystem.getInstance()
                .findFileByPath(modelCache)
            val fileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
            val virtualFileWrapper = fileSaverDialog.save(toSelect, "$modelCache/${cache.lastProject}Model$fileNameExt.json")
            if (virtualFileWrapper != null) {
                val fileToSave = virtualFileWrapper.file
                logsJTextArea.append("Save as file: ${fileToSave.absolutePath}\n")
                handler.saveModelToFile(models, fileToSave.absolutePath)
            }
        }

        modelControlPanel.add(analyzeButton)
        modelControlPanel.add(saveUmlFileButton)
        modelControlPanel.add(getUmlFileButton)
        mainJPanel.add(modelControlPanel)
    }

    private fun getRepoByUrl(url: String) {
        val repoName = buildModel.getRepoNameByUrl(url)
        val directoryPath = "$projectCache/$repoName"
        val directory = File(directoryPath)
        if (directory.exists() && directory.isDirectory) {
            logsJTextArea.append("Already exist!\n")
            myRepo = buildModel.getRepository(url, projectCache)
            if ((0 until projectComboBox.model.size).none { i -> projectComboBox.model.getElementAt(i) == repoName }) {
                projectComboBox.addItem(repoName)
            }
        } else {
            logsJTextArea.append("Cloning: ${url}\n")
            myRepo = buildModel.createClone(url, projectCache)
            logsJTextArea.append("Cloned to ${myRepo!!.path}\n")
            projectComboBox.addItem(repoName)
        }
        cache.projectPathMap[repoName] = ProjectPath(false, directoryPath, directoryPath)
        projectComboBox.selectedItem = repoName
    }

    private fun updatePathPanel() {
        val root = DefaultMutableTreeNode(cache.projectPathMap[cache.lastProject]?.path)
        buildTree(File(cache.projectPathMap[cache.lastProject]?.path), root)
        pathJTree.model = DefaultTreeModel(root)
        filesTreeJBScrollPane.setViewportView(pathJTree)
        filesTreeJBScrollPane.updateUI()
    }

    private fun buildTree(file: File, rootTree: DefaultMutableTreeNode) {
        file.list { dir, name -> File(dir, name).isDirectory }?.forEach {
            val path = "${file.absolutePath}/$it"
            val node = DefaultMutableTreeNode(path)
            rootTree.add(node)
            buildTree(File(path), node)
        }
        file.list { dir, name -> File(dir, name).isFile }?.forEach {
            val node = DefaultMutableTreeNode("${file.absolutePath}/$it")
            rootTree.add(node)
        }
    }


    private fun setupTree() {
        pathJTree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                val node = value as? DefaultMutableTreeNode ?: return
                val file = File(node.userObject.toString())
                if (file.isDirectory) {
                    icon = PlatformIcons.FOLDER_ICON
                } else
                    icon = FileTypeManager.getInstance().getFileTypeByFileName(file.name).icon
                append(file.name)
            }
        }

        pathJTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e!!.clickCount == 2) {
                    val selRow = pathJTree.getRowForLocation(e.x, e.y)
                    if (selRow != -1) {
                        val selPath = pathJTree.getPathForLocation(e.x, e.y)
                        val selectedNode = selPath?.lastPathComponent as? DefaultMutableTreeNode
                        onTreeNodeDoubleClicked(selectedNode)
                    }
                }
            }
        })
    }
}

private fun onTreeNodeDoubleClicked(node: DefaultMutableTreeNode?) {
    val filePath = node?.userObject.toString()
    val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
    if (virtualFile != null) {
        // TODO: should be current project (not first)
        ProjectManager.getInstance().openProjects.firstOrNull()
            ?.let { FileEditorManager.getInstance(it).openFile(virtualFile, true) }
    }
}

class CustomSplitPaneUI : BasicSplitPaneUI() {
    override fun createDefaultDivider(): BasicSplitPaneDivider {
        return object : BasicSplitPaneDivider(this) {
            override fun paint(g: Graphics) {
                val bgColor = UIUtil.getPanelBackground()
                val borderColor = ColorUtil.darker(bgColor, 1)
                g.color = borderColor
                g.fillRect(0, 0, size.width, size.height)
                super.paint(g)
            }
        }
    }
}