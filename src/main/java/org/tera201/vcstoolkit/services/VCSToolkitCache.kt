package org.tera201.vcstoolkit.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "VCSToolkitCachesNew", storages = [Storage("vcs_toolkit_cache.xml")])
class VCSToolkitCache : PersistentStateComponent<VCSToolkitCache> {
    var lastProject: String = ""
    var projectPathMap:MutableMap<String, String> = hashMapOf()


    companion object {
        fun getInstance(): VCSToolkitCache {
            return ApplicationManager.getApplication().getService(VCSToolkitCache::class.java)
        }
    }

    override fun getState(): VCSToolkitCache {
        return this
    }

    override fun loadState(state: VCSToolkitCache) {
        XmlSerializerUtil.copyBean(state, this)
        ApplicationManager.getApplication().messageBus.syncPublisher(CacheChangedListener.TOPIC)
            .onCacheChange(this)
    }

    interface CacheChangedListener {

        companion object {
            val TOPIC = Topic.create(
                "VCSToolkitApplicationCacheChanged",
                CacheChangedListener::class.java
            )
        }

        fun onCacheChange(cache: VCSToolkitCache)
    }

}