package com.xiaopeng.xposed.instrument.compass.ribbon.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat

@SuppressLint("DiscouragedApi")
fun <T : View> ViewGroup.findViewById(id: String): T {
    return findViewById(resources.getIdentifier(id, "id", context.packageName))
}

@SuppressLint("DiscouragedApi")
fun Context.getColorStateList(id: String): ColorStateList {
    return ContextCompat.getColorStateList(this, resources.getIdentifier(id, "color", this.packageName))!!
}

@SuppressLint("DiscouragedApi")
fun Context.getColor(id: String): Int {
    return ContextCompat.getColor(this, resources.getIdentifier(id, "color", this.packageName))
}

@SuppressLint("DiscouragedApi")
fun Context.getStyle(id: String): Int {
    return resources.getIdentifier(id, "style", packageName)
}

@SuppressLint("DiscouragedApi")
fun Context.getString(id: String): String {
    return resources.getString(resources.getIdentifier(id, "string", packageName))
}


