package com.xiaopeng.xposed.instrument.compass.ribbon.extensions

import android.content.Context

fun Int.toDp(context: Context): Int {
    return (this * context.resources.displayMetrics.density + 0.5f).toInt()
}
