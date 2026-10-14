package com.moneytracker.app.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * 共同資料層協調器 (DataOperationCoordinator)
 * 負責協調前景資料寫入 (新增/修改/刪除帳戶、交易、借貸、還款)、備份 (建立快照與上傳)、還原 (覆蓋寫入)，
 * 同時涵蓋前景 ViewModel 與背景 BackupWorker，避免資料競爭與快照漏抓問題。
 */
object DataOperationCoordinator {
    private val mutex = Mutex()
    private val activeWriteCount = AtomicInteger(0)

    /**
     * 執行一般資料寫入操作 (新增/修改/刪除)
     */
    suspend fun <T> runWriteOperation(block: suspend () -> T): T {
        activeWriteCount.incrementAndGet()
        return try {
            mutex.withLock {
                block()
            }
        } finally {
            activeWriteCount.decrementAndGet()
        }
    }

    /**
     * 執行備份操作：必須等待進行中之寫入完成後，再建立交易快照與進行上傳
     */
    suspend fun <T> runBackupOperation(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }

    /**
     * 執行還原操作：取得獨占寫入鎖，防止還原過程中發生其他寫入或備份
     */
    suspend fun <T> runRestoreOperation(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }

    fun hasPendingWrites(): Boolean = activeWriteCount.get() > 0
}
