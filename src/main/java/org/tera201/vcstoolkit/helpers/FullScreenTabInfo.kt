package org.tera201.vcstoolkit.helpers

import com.intellij.openapi.vfs.VirtualFile
import javafx.embed.swing.JFXPanel

data class FullScreenTabInfo(val index: Int, val jfxPanel: JFXPanel, val virtualFile: VirtualFile)
