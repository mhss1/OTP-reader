package com.mhss.app.otpreader.data.listener

import android.content.ClipData
import android.content.ClipboardManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mhss.app.otpreader.data.datastore.DataStoreRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsListener : NotificationListenerService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @Inject
    lateinit var dataStore: DataStoreRepository

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn?.notification
        val extras = notification?.extras
        val packageName = sbn?.packageName
        val title = extras?.getString("android.title")
        val text = extras?.getString("android.text")

        serviceScope.launch {
            val packages = dataStore.get(DataStoreRepository.PACKAGES, emptySet()).first()

            if (packages.contains(packageName)) {
                val mustContain = dataStore.get(DataStoreRepository.CONTAINS, emptySet()).first()
                if (mustContain.isEmpty()) {
                    text?.getOTP()?.copyToClipboard()
                } else {
                    val opt = if (mustContain.any { text?.contains(it) == true }){
                        text?.getOTP()
                    } else if (mustContain.any { title?.contains(it) == true }) {
                        title?.getOTP()
                    } else null

                    opt?.copyToClipboard()
                }
            }
        }
    }

    private fun String.getOTP(): String? {
        // 4 to 8 digit OTP
        val pattern = Pattern.compile("\\b\\d{4,8}\\b")

        val matcher = pattern.matcher(this)

        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    private fun String.copyToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("otp", this)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}