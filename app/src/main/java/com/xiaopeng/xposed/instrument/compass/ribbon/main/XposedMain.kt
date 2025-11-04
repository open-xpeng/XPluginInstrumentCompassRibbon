package com.xiaopeng.xposed.instrument.compass.ribbon.main

import com.xiaopeng.xposed.instrument.compass.ribbon.commons.HostFirstClassLoader
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedMain : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        HostFirstClassLoader.injectClassLoader(hostClassLoader = requireNotNull(lpparam.classLoader))
        val main: IXposedHookLoadPackage? = when (lpparam.packageName) {
            "com.xiaopeng.montecarlo" -> XposedMainMontecarlo()
            "com.xiaopeng.instrument" -> XposedMainInstrument()
            else                      -> null
        }
        main?.handleLoadPackage(lpparam)
    }
}
