package com.xiaopeng.xposed.instrument.compass.ribbon.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.xiaopeng.xposed.instrument.compass.ribbon.R
import com.xiaopeng.xposed.instrument.compass.ribbon.aidl.IXposedCallback
import com.xiaopeng.xposed.instrument.compass.ribbon.aidl.IXposedInterface
import com.xiaopeng.xposed.instrument.compass.ribbon.commons.HostFirstClassLoader
import com.xiaopeng.xposed.instrument.compass.ribbon.constants.ConstantApp
import com.xiaopeng.xposed.instrument.compass.ribbon.extensions.findViewById
import com.xiaopeng.xposed.instrument.compass.ribbon.extensions.getColor
import com.xiaopeng.xposed.instrument.compass.ribbon.extensions.toDp
import com.xiaopeng.xposed.instrument.compass.ribbon.widget.CompassRibbonView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.joor.Reflect

class XposedMainInstrument : IXposedHookLoadPackage {

    companion object {
        private const val TAG: String = "XposedMainInstrument"
    }

    private val mCompassRibbonViewId: Int = View.generateViewId()
    private val mMapStatusChangedLiveData: MutableLiveData<Float> = MutableLiveData()

    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod("com.xiaopeng.instrument.viewmodel.ControlViewModel", loadPackageParam.classLoader, "onNaviStart", mHookMethodControlViewModelOnNaviStart)
        XposedHelpers.findAndHookMethod("com.xiaopeng.instrument.widget.AbstractBottomStatusBarView", loadPackageParam.classLoader, "initContentView", mHookMethodAbstractBottomStatusBarViewInitContentView)
        XposedHelpers.findAndHookMethod("com.xiaopeng.instrument.widget.AbstractBottomStatusBarView", loadPackageParam.classLoader, "changeTheme", mHookMethodAbstractBottomStatusBarViewChangeTheme)
    }

    private val mHookMethodControlViewModelOnNaviStart: XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            try {
                afterHookedMethodCatching(param)
            } catch (t: Throwable) {
                Log.w(TAG, "HookMethodControlViewModelOnNaviStart: ", t)
            }
        }

        private fun afterHookedMethodCatching(param: MethodHookParam) {
            val intent = Intent()
            intent.setAction(ConstantApp.ACTION_MAP_STATUS_CHANGED)
            intent.setClassName(ConstantApp.PACKAGE_NAME, ConstantApp.CLASS_NAME)

            val thisObject: Any = param.thisObject
            val context: Context = Reflect.onClass("com.xiaopeng.instrument.App", thisObject.javaClass.classLoader).call("getInstance").get()
            context.bindService(intent, mMinMapServiceConnection, Context.BIND_AUTO_CREATE)
        }

        private val mMinMapServiceConnection: ServiceConnection = object : ServiceConnection {
            private var mXposedInterface: IXposedInterface? = null

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.w(TAG, "HookMethodControlViewModelOnNaviStart: onServiceConnected: name=$name, service=$service")
                mXposedInterface = IXposedInterface.Stub.asInterface(service)
                mXposedInterface!!.registerListener(mOnMiniMapStatusChanged)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w(TAG, "HookMethodControlViewModelOnNaviStart: onServiceDisconnected: name=$name")
                mXposedInterface?.unregisterListener(mOnMiniMapStatusChanged)
                mXposedInterface = null
            }

            override fun onBindingDied(name: ComponentName?) {
                super.onBindingDied(name)
                mXposedInterface?.unregisterListener(mOnMiniMapStatusChanged)
                mXposedInterface = null
            }
        }

        private val mOnMiniMapStatusChanged: IXposedCallback = object : IXposedCallback.Stub() {
            override fun onMapStatusChanged(mapAngle: Float, carDir: Float, drawMode: Int) {
                when (drawMode) {
                    0    -> mMapStatusChangedLiveData.postValue(carDir)
                    1    -> mMapStatusChangedLiveData.postValue(mapAngle)
                    2    -> mMapStatusChangedLiveData.postValue(mapAngle)
                    else -> mMapStatusChangedLiveData.postValue(carDir)
                }
            }
        }

    }

    private val mHookMethodAbstractBottomStatusBarViewInitContentView: XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                afterHookedMethodCatching(param)
            } catch (t: Throwable) {
                Log.w(TAG, "HookMethodAbstractBottomStatusBarViewInitContentView: ", t)
            }
        }

        private fun afterHookedMethodCatching(param: MethodHookParam) {
            val thisObject = param.thisObject as RelativeLayout

            // com.xiaopeng.xui.widget.XConstraintLayout -> androidx.constraintlayout.widget.ConstraintLayout
            val constraintLayout: ViewGroup = thisObject.findViewById(id = "bottom_bar_view")
            initContentView(view = constraintLayout)
        }

        private fun initContentView(view: ViewGroup) {
            val context: Context = view.context
            context as AppCompatActivity

            val tvRemainDistanceUnit: TextView = view.findViewById(id = "tv_remain_distance_unit")
            val compassRibbonView = CompassRibbonView(context)
            compassRibbonView.id = mCompassRibbonViewId
            compassRibbonView.layoutParams = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                topToTop = tvRemainDistanceUnit.id
                bottomToBottom = tvRemainDistanceUnit.id
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID

                marginStart = 700.toDp(context)
                marginEnd = 700.toDp(context)
            }
            compassRibbonView.setTextSize(sizeInPx = tvRemainDistanceUnit.textSize)
            compassRibbonView.setTextColor(color = context.getColor(id = "bottom_bar_common_text_color"))
            view.addView(compassRibbonView)

            val observer = MapStatusChangedObserver(view = compassRibbonView)
            mMapStatusChangedLiveData.observe(context, observer)
        }

    }

    private val mHookMethodAbstractBottomStatusBarViewChangeTheme: XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val thisObject = param.thisObject as RelativeLayout
            val isNightMode: Boolean = Reflect.onClass("com.xiaopeng.libtheme.ThemeManager", param.thisObject.javaClass.classLoader).call("isNightMode", thisObject.context).get()

            // night -> <color name="bottom_bar_common_text_color">#dbffffff</color>
            // day   -> <color name="bottom_bar_common_text_color">#db0f1520</color>
            val compassRibbonView: CompassRibbonView? = thisObject.findViewById(mCompassRibbonViewId)
            compassRibbonView?.setTextColor(color = if (isNightMode) 0xdbffffff.toInt() else 0xdb0f1520.toInt())
        }
    }

    private class MapStatusChangedObserver(private val view: CompassRibbonView) : Observer<Float> {
        override fun onChanged(value: Float) {
            view.setValue(angle = value)
        }
    }

}
