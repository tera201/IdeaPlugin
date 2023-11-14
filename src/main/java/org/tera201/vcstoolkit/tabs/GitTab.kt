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
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import javafx.application.Platform
import model.console.BuildModel
import org.eclipse.uml2.uml.Model
import org.repodriller.scm.SCMRepository
import org.tera201.code2uml.AnalyzerBuilder
import org.tera201.code2uml.Language
import org.tera201.code2uml.uml.util.UMLModelHandler
import org.tera201.vcstoolkit.helpers.ProjectPath
import org.tera201.vcstoolkit.helpers.SharedModel
import org.tera201.vcstoolkit.services.VCSToolkitCache
import org.tera201.vcstoolkit.services.settings.VCSToolkitSettings
import org.tera201.vcstoolkit.utils.toCircle
import java.awt.*
import java.awt.event.*
import java.io.File
import java.io.IOException
import javax.swing.*
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
    // TODO: problems with JBTextArea (IDEA freeze) when analyzing one branch
    private val logsJTextArea = JTextArea().apply {
        this.isEditable = false
    }
    val logsJBScrollPane = JBScrollPane(
        logsJTextArea,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
    companion object {
        var models = ArrayList<Model>()
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
    private val branchPane = JPanel().apply {
        val label = JLabel("Branches")
        this.layout = BorderLayout()
        this.add(label, BorderLayout.NORTH)
        this.add(branchListScrollPane, BorderLayout.CENTER)
    }
    private val tagListScrollPane = JBScrollPane(tagList)
    private val tagPane = JPanel().apply {
        val label = JLabel("Tags")
        this.layout = BorderLayout()
        this.add(label, BorderLayout.NORTH)
        this.add(tagListScrollPane, BorderLayout.CENTER)
    }
    private val modelListScrollPane = JBScrollPane(modelList)
    val pathJTree = Tree()
    private val projectComboBox = ComboBox<String>()
    private val openProjectButton = JButton("Open project")
    private val vcSplitPane = JBSplitter(true, 0.5f).apply {
        this.firstComponent = branchPane
        this.secondComponent = tagPane
        this.dividerWidth = 1
    }
    private val filesTreeJBScrollPane =
        JBScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
    private val currentBranchOrTagLabel = JLabel("Current")
    private val filesTreePane = JPanel().apply {
        this.layout = BorderLayout()
        this.add(currentBranchOrTagLabel, BorderLayout.NORTH)
        this.add(filesTreeJBScrollPane, BorderLayout.CENTER)
    }
    private val showSplitPane = JBSplitter(false, 0.5f).apply {
        this.firstComponent = filesTreePane
        this.secondComponent = vcSplitPane
        this.dividerWidth = 1
    }
    private val logModelSplitPane = JBSplitter(false, 0.5f).apply {
        this.firstComponent = logsJBScrollPane
        this.secondComponent = modelListScrollPane
    this.dividerWidth = 1
    }
    private var analyzing = false
    private val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("VCSToolkitNotify")
    
    init {
        initGit()
    }

    private fun initGit() {

        try {
            File(settings.repoPath).mkdirs()
            File(settings.modelPath).mkdirs()
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
                        projectComboBox.selectedItem = cache.lastProject
                    }
                }
            })

        urlField.text = cache.urlField
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
                    cache.urlField = urlField.text
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
        addModelControlPanel()
        addProjectPane()
    }

    private fun addProjectPane() {
        cache.projectPathMap["Current project"] = ProjectPath(true, ProjectManager.getInstance().openProjects[0].basePath.toString(), "${settings.repoPath}/${ProjectManager.getInstance().openProjects[0].name}")

        if (cache.projectPathMap.isNotEmpty()) {
            cache.projectPathMap.keys.forEach {
                projectComboBox.addItem(it)
            }
            if (cache.lastProject != "") {
                projectComboBox.selectedItem = cache.lastProject
                cache.projectPathMap[cache.lastProject]?.let { updatePathPanelAndGitLists(cache.lastProject, it.path) }
            }
        }

        File(settings.repoPath).list { dir, name -> File(dir, name).isDirectory.and(name != "Models") }?.forEach {
            val path = "${settings.repoPath}/$it"
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
                createNotification("Oops", "The project with the specified path was not found.", NotificationType.ERROR)
            }
            cache.lastProject = selectedProject
        }

        openProjectButton.addActionListener {
            val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            val toSelect = if (settings.repoPath.isEmpty()) null else LocalFileSystem.getInstance()
                .findFileByPath(settings.repoPath)
            val selectedDirectory = FileChooser.chooseFile(descriptor, null, toSelect)
            if (selectedDirectory != null) {
                val newProjectName = selectedDirectory.name
                val copyPath = "${settings.repoPath}/${selectedDirectory.name}"
                cache.projectPathMap[newProjectName] = ProjectPath(true, selectedDirectory.path, copyPath)
                projectComboBox.addItem(newProjectName)
                projectComboBox.selectedItem = newProjectName
            }
        }

        this.add(projectComboBox)
        this.add(openProjectButton)
    }

    private fun createNotification(title:String, message:String, notificationType: NotificationType) {
        val notification: Notification =
            notificationGroup.createNotification("VCSToolkit - $title", message, notificationType)
        Notifications.Bus.notify(notification, null)
    }

    private fun createExceptionNotification(e:Exception) {
        val content = "message: ${e.message}\nstackTrace: ${e.stackTrace.joinToString("\n")}"
        createNotification(e.javaClass.simpleName, content, NotificationType.ERROR)
    }

    private fun externalWarningNotification() {
        createNotification("warning", "VCS for external projects was disabled. " +
                "Please use the built-in VCS in the IDE manually.", NotificationType.WARNING)
    }

    private fun updatePathPanelAndGitLists(projectName: String, projectPath: String) {
        thread {
            val isRepo = File("$projectPath/.git").exists()
            if (myRepo?.repoName != projectName && isRepo) myRepo = buildModel.getRepository(projectPath)
            updatePathPanel()
            if (isRepo && !(cache.projectPathMap[projectName]!!.isExternal && settings.externalProjectMode == 1)) {
                populateJBList(branchListModel, buildModel.getBranches(myRepo).filter { it != "HEAD" })
                populateJBList(tagListModel, buildModel.getTags(myRepo))
                if (myRepo?.scm?.currentBranchOrTagName != null)
                    currentBranchOrTagLabel.text = myRepo?.scm?.currentBranchOrTagName
            } else if (cache.projectPathMap[projectName]!!.isExternal && settings.externalProjectMode == 1) {
                currentBranchOrTagLabel.text = getProjectNameOrCurrentBranchOrTag(projectPath, isRepo)
                branchListModel.clear()
                tagListModel.clear()
                externalWarningNotification()
            } else {
                createNotification("Opps", "The project doesn't have git repo.", NotificationType.WARNING)
                currentBranchOrTagLabel.text = getProjectNameOrCurrentBranchOrTag(projectPath, isRepo)
                branchListModel.clear()
                tagListModel.clear()
            }
        }
    }

    private fun getProjectNameOrCurrentBranchOrTag(projectPath: String, isRepo:Boolean):String {
        val splitPath = projectPath.split("/")
        val lastDirectory = splitPath[splitPath.size - 1]
        var name = lastDirectory
        if (isRepo) {
            myRepo = buildModel.getRepository(projectPath)
            name = myRepo!!.scm.currentBranchOrTagName
        }
        return name
    }

    private fun configureSplitPanes() {
        setupListSelectionListeners()
        setupListDoubleClickAction()
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
                if (e!!.clickCount == 2 && !analyzing) {
                    val index = jbList.locationToIndex(e.point)
                    if (index >= 0) {
                        val selectedItem = jbList.model.getElementAt(index)
                        onListItemDoubleClicked(selectedItem)
                    }
                } else if (analyzing) {
                    createNotification("Checkout freeze",
                        "You cannot checkout while the analyzer is running", NotificationType.WARNING)
                }
            }
        }
    }

    private fun onListItemDoubleClicked(item: String) {
        println("Double clicked on item: $item")
        checkoutTo(item)
    }

    private fun checkoutTo(item:String) {
        val fileSystem = LocalFileSystem.getInstance()
        val virtualFile: VirtualFile? = fileSystem.findFileByPath(myRepo!!.path)
        val virtualFileGit: VirtualFile? = fileSystem.findFileByPath("${myRepo!!.path}/.git}")
        try {
            if (settings.externalProjectMode == 2)
                myRepo?.scm?.createCommit("VCSToolkit: save message")
            buildModel.checkout(myRepo, item)
            if (myRepo?.scm?.currentBranchOrTagName != null)
                currentBranchOrTagLabel.text = myRepo?.scm?.currentBranchOrTagName
            if (settings.externalProjectMode == 2)
                myRepo?.scm?.resetLastCommitsWithMessage("VCSToolkit: save message")
        } catch (e:Exception) {
            createExceptionNotification(e)
        }
        virtualFile?.refresh(false, true)
        virtualFileGit?.refresh(false, true)
        updatePathPanel()
    }

    private fun populateJBList(targetListModel: DefaultListModel<String>, stringList: List<String>) {
        targetListModel.clear()
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

    private fun addModelControlPanel() {
        val modelControlPanel = JPanel()
        analyzeButton.addActionListener {
            if (myRepo != null) {
                thread {
                    val projectPath = cache.projectPathMap[cache.lastProject]?.path
                    analyzing = true
                    val startTime = System.currentTimeMillis()
                    val allList = branchList.selectedValuesList + tagList.selectedValuesList
                    models.clear()
                    modelListContent.clear()
                    logsJTextArea.append("Start analyzing.\n")
                    for (i in allList) {
                        logsJTextArea.append("\t*modeling: $i\n")
                        logsJTextArea.caret.dot = logsJTextArea.text.length
                        checkoutTo(i)
                        try {
                            val analyzerBuilder = AnalyzerBuilder(Language.Java, i, myRepo!!.path)
                                .textArea(logsJTextArea).threads(4)
                            models.add(analyzerBuilder.build())
                        } catch (e:Exception) {
                            createExceptionNotification(e)
                        }
                    }
                    if (allList.size == 1) saveUmlFileButton.text = "Save UML model"
                    else saveUmlFileButton.text = "Save UML model pack"
                    if (allList.isEmpty() && projectPath != null) {
                        saveUmlFileButton.text = "Save UML model"
                        logsJTextArea.append("\t*modeling: ${currentBranchOrTagLabel.text}\n")
                        try {
                            val analyzerBuilder =
                                AnalyzerBuilder(Language.Java, currentBranchOrTagLabel.text, projectPath)
                                    .textArea(logsJTextArea).threads(4)
                            models.add(analyzerBuilder.build())
                        } catch (e:Exception) {
                            createExceptionNotification(e)
                        }
                    }
                    val endTime = System.currentTimeMillis()
                    val executionTime = (endTime - startTime) / 1000.0
                    logsJTextArea.append("End analyzing. Execution time: $executionTime sec.\n")
                    analyzing = false
                    modelListContent.addAll(models.stream().map { it.name }.toList())

                    Platform.runLater {
                        try {
                            FXCircleTab.circleSpace.clean()
                            for (i in 0 until models.size) {
                                models[i].toCircle(i)
                            }
                            FXCircleTab.circleSpace.updateView()
                        } catch (e:Exception) {
                            createExceptionNotification(e)
                        }
                    }
                }
            } else logsJTextArea.append("Get some repo for analyzing.\n")
        }

        getUmlFileButton.addActionListener {
            val descriptor = FileChooserDescriptor(
                true, false,
                false, false, false, false
            );
            descriptor.setTitle("Get UML-Model");
            val toSelect = if (settings.modelPath.isEmpty()) null else LocalFileSystem.getInstance()
                .findFileByPath(settings.modelPath)
            val virtualFile = FileChooser.chooseFile(descriptor, null, toSelect)

            if (virtualFile != null) {
                logsJTextArea.append("Get model from file: ${virtualFile.path}\n")
                try {
                    val modelList = handler.loadListModelFromFile(virtualFile.path)
                    if (modelList != null)
                        models = modelList as ArrayList<Model>
                    modelListContent.clear()
                    modelListContent.addAll(models.stream().map { it.name }.toList())

                    Platform.runLater {
                        try {
                            FXCircleTab.circleSpace.clean()
                            for (i in 0 until models.size) {
                                models[i].toCircle(i)
                            }
                            FXCircleTab.circleSpace.mainListObjects.forEach { it.updateView() }
                        } catch (e:Exception) {
                            createExceptionNotification(e)
                        }
                    }
                } catch (e: Exception) {
                    createExceptionNotification(e)
                }
            }
        }
//
        saveUmlFileButton.addActionListener {
            val title =  if (models.size == 1) "SAVE UML-MODEL" else "SAVE UML-MODEL PACK"
            val fileNameExt = if (models.size == 1) "" else "Pack"
            val descriptor = FileSaverDescriptor(
                title, "Choose the destination file",
                "json"
            );
            val toSelect = if (settings.modelPath.isEmpty()) null else LocalFileSystem.getInstance()
                .findFileByPath(settings.modelPath)
            val fileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
            val fileName = if(SystemInfo.isMac) "${cache.lastProject}Model$fileNameExt.json"
            else "${cache.lastProject}Model$fileNameExt"
            val virtualFileWrapper = fileSaverDialog.save(toSelect, fileName)
            if (virtualFileWrapper != null) {
                val fileToSave = virtualFileWrapper.file
                logsJTextArea.append("Save as file: ${fileToSave.absolutePath}\n")
                try {
                    handler.saveModelToFile(models, fileToSave.absolutePath)
                } catch (e: Exception) {
                    createExceptionNotification(e)
                }
            }
        }

        modelControlPanel.add(analyzeButton)
        modelControlPanel.add(saveUmlFileButton)
        modelControlPanel.add(getUmlFileButton)
        this.add(modelControlPanel)
    }

    private fun getRepoByUrl(url: String) {
        val repoName = buildModel.getRepoNameByUrl(url)
        val directoryPath = "${settings.repoPath}/$repoName"
        val directory = File(directoryPath)
        try {
            if (directory.exists() && directory.isDirectory) {
                logsJTextArea.append("Already exist!\n")
                myRepo = buildModel.getRepository(url, settings.repoPath)
                if ((0 until projectComboBox.model.size).none { i -> projectComboBox.model.getElementAt(i) == repoName }) {
                    projectComboBox.addItem(repoName)
                }
            } else {
                logsJTextArea.append("Cloning: ${url}\n")
                if (settings.username.equals(""))
                    myRepo = buildModel.createClone(url, settings.repoPath)
                else myRepo = buildModel.createClone(url, settings.repoPath, settings.username, settings.password)
                logsJTextArea.append("Cloned to ${myRepo!!.path}\n")
                projectComboBox.addItem(repoName)
            }
        } catch (e: Exception) {
            createExceptionNotification(e)
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