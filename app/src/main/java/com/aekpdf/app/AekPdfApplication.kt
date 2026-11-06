package com.aekpdf.app

import android.app.Application
import com.aekpdf.app.core.database.AppDatabase

class AekPdfApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AekPdfApplication
            private set
    }
}
