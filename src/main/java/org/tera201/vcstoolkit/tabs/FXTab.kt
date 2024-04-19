package org.tera201.vcstoolkit.tabs

import javafx.embed.swing.JFXPanel

interface FXTab {
    fun setJFXPanel(panel: JFXPanel)

    fun setExpandMode();
    fun setCollapseMode();
}