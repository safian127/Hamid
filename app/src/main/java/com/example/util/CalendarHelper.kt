package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import java.util.TimeZone

fun addTaskToDeviceCalendar(context: Context, title: String, description: String, dueDateMs: Long): Long? {
    return try {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val uri = CalendarContract.Calendars.CONTENT_URI
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        var calendarId: Long = 1
        if (cursor != null && cursor.moveToFirst()) {
            calendarId = cursor.getLong(0)
            cursor.close()
        }
        
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, dueDateMs)
            put(CalendarContract.Events.DTEND, dueDateMs + 3600000) // 1 hour duration
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        
        val eventUri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        eventUri?.lastPathSegment?.toLongOrNull()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
