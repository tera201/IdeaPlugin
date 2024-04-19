package org.tera201.vcstoolkit.tabs

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import org.tera201.vcstoolkit.panels.FXCirclePanel
import org.eclipse.uml2.uml.Model
import org.tera201.vcstoolkit.utils.toCircle
import java.awt.BorderLayout
import javax.swing.JPanel


class FXCircleTab(val tabManager: TabManager) : JPanel(), FXTab {
    var fxCircle = FXCirclePanel()
    init {
        this.layout = BorderLayout()
        this.add(fxCircle, BorderLayout.CENTER)
    }

    fun renderByModel(models: ArrayList<Model>) {
        Platform.runLater {
            fxCircle.circleSpace.clean()
            for (i in 0 until models.size) {
                models[i].toCircle(fxCircle.circleSpace, i)
            }
            fxCircle.circleSpace.updateView()
        }
    }

    override fun setJFXPanel(panel:JFXPanel) {
        this.add(panel, BorderLayout.CENTER)
    }

    override fun setExpandMode() {
        TODO("Not yet implemented")
    }

    override fun setCollapseMode() {
        TODO("Not yet implemented")
    }
}