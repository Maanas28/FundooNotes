package com.example.fundoonotes.UI.features.addnote.util


import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class ReminderManager(
    private val context: Context,
    private val badge: TextView,
    private var onReminderSet: (Long?) -> Unit
) {
    private val calendar = Calendar.getInstance()

    fun showPicker() {
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute, 0)
                val timeInMillis = calendar.timeInMillis
                val formatted = format(timeInMillis)
                badge.text = formatted
                badge.visibility = TextView.VISIBLE
                onReminderSet(timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun removeReminder() {
        badge.visibility = TextView.GONE
        onReminderSet(null)
    }

    fun format(time: Long): String =
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(time))
}
