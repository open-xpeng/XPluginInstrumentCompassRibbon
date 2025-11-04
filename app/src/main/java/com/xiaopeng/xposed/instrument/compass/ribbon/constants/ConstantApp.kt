package com.xiaopeng.xposed.instrument.compass.ribbon.constants

object ConstantApp {

    const val PACKAGE_NAME: String = "com.xiaopeng.montecarlo"
    const val CLASS_NAME: String = "com.xiaopeng.montecarlo.service.minimap.MiniMapService"

    // 原版actions
    const val ACTION_MAP_SURFACE_CHANGED: String = "com.xiaopeng.montecarlo.minimap.ACTION_MAP_SURFACE_CHANGED"
    const val ACTION_MAP_SURFACE_CREATE: String = "com.xiaopeng.montecarlo.minimap.ACTION_MAP_SURFACE_CREATE"
    const val ACTION_MAP_SURFACE_DESTROY: String = "com.xiaopeng.montecarlo.minimap.ACTION_MAP_SURFACE_DESTROY"

    // 自定义actions
    const val ACTION_MAP_STATUS_CHANGED: String = "com.xiaopeng.montecarlo.minimap.ACTION_MAP_STATUS_CHANGED"

}
