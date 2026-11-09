package com.example.sharedexpenses

import android.app.Application

/** Registered in AndroidManifest.xml. Holds the AppContainer so ViewModels and the SyncWorker can reach it. */
class SharedExpensesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
