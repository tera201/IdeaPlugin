package org.tera201.vcstoolkit.services.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "VCSToolkitSettingsNew", storages = [Storage("vcs_toolkit_settings.xml")])
class VCSToolkitSettings : PersistentStateComponent<VCSToolkitSettings> {

    var repoPath:String =  "${System.getProperty("user.dir")}/VCSToolkitCache"

    var modelPath:String = "${System.getProperty("user.dir")}/VCSToolkitCache/Models"
    var showGitLogs:Boolean = true
    var externalProjectMode:Int = 0


    companion object {
        fun getInstance(): VCSToolkitSettings {
            return ApplicationManager.getApplication().getService(VCSToolkitSettings::class.java)
        }
    }

    override fun getState(): VCSToolkitSettings {
        return this
    }

    override fun loadState(state: VCSToolkitSettings) {
        XmlSerializerUtil.copyBean(state, this)
        ApplicationManager.getApplication().messageBus.syncPublisher(SettingsChangedListener.TOPIC)
            .onSettingsChange(this)
    }

    interface SettingsChangedListener {

        companion object {
            val TOPIC = Topic.create(
                "VCSToolkitApplicationSettingsChanged",
                SettingsChangedListener::class.java
            )
        }

        fun onSettingsChange(settings: VCSToolkitSettings)
    }
}