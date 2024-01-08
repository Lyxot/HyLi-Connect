package xyz.hyli.connect.datastore

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

/**
 * @author Dylan Cai
 */
class DataStoreInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        IDataStoreOwner.application = context as Application
    }

    override fun dependencies() = emptyList<Class<Initializer<*>>>()
}