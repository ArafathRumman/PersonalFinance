package com.taka.personalfinance

import android.app.Application
import com.taka.personalfinance.data.AppDatabase
import com.taka.personalfinance.data.FinanceRepository
import com.taka.personalfinance.util.AppLock
import com.taka.personalfinance.util.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PFApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: FinanceRepository
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var appLock: AppLock
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        repository = FinanceRepository(database.dao())
        settings = AppSettings(this)
        appLock = AppLock(this)
        appScope.launch {
            try {
                repository.seedIfNeeded()
            } catch (e: Exception) {
                // Categories will be created on the next launch.
            }
        }
    }
}
