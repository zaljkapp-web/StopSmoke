package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlin.random.Random

class SmokingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        createNotificationChannel(context)

        when (action) {
            ACTION_TIMER_EXPIRED -> {
                val funnyMessage = FUNNY_MESSAGES[Random.nextInt(FUNNY_MESSAGES.size)]
                showNotification(context, "SmokeShift - Idő van!", funnyMessage, NOTIFICATION_ID_TIMER)
            }
            ACTION_SHIFT_START -> {
                showNotification(
                    context, 
                    "SmokeShift - Műszak Elkezdődött!", 
                    "Kezdődik a műszak! A tüdődet is munkára fogjuk: újraszámolva a műszakos cigiadag!", 
                    NOTIFICATION_ID_SHIFT_START
                )
            }
            ACTION_SHIFT_END -> {
                showNotification(
                    context, 
                    "SmokeShift - Műszak Vége!", 
                    "Letetted a lantot! Gratulálunk, itt a megérdemelt műszak végi cigid. Szívd egészséggel (vagy nem)!", 
                    NOTIFICATION_ID_SHIFT_END
                )
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SmokeShift Notifications"
            val descriptionText = "Értesítések dohányzási időzítőkhöz és műszakokhoz"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Using system icon for simplicity
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "smokeshift_channel"
        const val ACTION_TIMER_EXPIRED = "com.example.smokeshift.ACTION_TIMER_EXPIRED"
        const val ACTION_SHIFT_START = "com.example.smokeshift.ACTION_SHIFT_START"
        const val ACTION_SHIFT_END = "com.example.smokeshift.ACTION_SHIFT_END"

        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_SHIFT_START = 1002
        const val NOTIFICATION_ID_SHIFT_END = 1003

        val FUNNY_MESSAGES = listOf(
            "Na, mehetsz mérgezni magad. A tüdőd már nagyon hiányolta a kátrányt.",
            "Engedély megadva! Szippants egy kis rákrudat, megérdemled...",
            "Idő van! Rohanj, mielőtt még véletlenül tisztulni kezdene a szervezeted.",
            "Gratulálok, túlélted a legutóbbi óta eltelt időt. Itt a jutalmad: még egy szög a koporsódba!",
            "A visszaszámláló lejárt. Füstölj el egy kis pénzt, a dohánygyárak hálásak lesznek.",
            "Na végre, pöfékelhetsz! Már kezdtem aggódni, hogy egészséges leszel.",
            "Kopp-kopp, a nikotinmanó kopogtat. Nyisd ki a tüdőkaput!",
            "Készen állsz a tüdőpusztításra? A számláló lejárt, tüzet!"
        )
    }
}
