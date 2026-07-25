package fr.actus.sync

import android.app.Application
import fr.actus.sync.data.CookieRepositoryHolder

class ActusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CookieRepositoryHolder.init(this)
    }
}
