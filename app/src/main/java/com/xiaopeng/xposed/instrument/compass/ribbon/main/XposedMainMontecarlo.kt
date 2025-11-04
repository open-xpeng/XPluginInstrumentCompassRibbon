package com.xiaopeng.xposed.instrument.compass.ribbon.main

import android.content.Intent
import android.os.RemoteCallbackList
import android.util.Log
import com.xiaopeng.xposed.instrument.compass.ribbon.aidl.IXposedCallback
import com.xiaopeng.xposed.instrument.compass.ribbon.aidl.IXposedInterface
import com.xiaopeng.xposed.instrument.compass.ribbon.constants.ConstantApp
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.joor.Reflect

class XposedMainMontecarlo : IXposedHookLoadPackage {

    companion object {
        private const val TAG: String = "XposedMainMontecarlo"
    }

    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod("com.xiaopeng.montecarlo.views.mapmode.DrawingThread", loadPackageParam.classLoader, "getMapStatus", mHookMethodDrawingThreadGetMapStatus)
        XposedHelpers.findAndHookMethod("com.xiaopeng.montecarlo.service.minimap.MiniMapService", loadPackageParam.classLoader, "onBind", Intent::class.java, mHookMethodMiniMapServiceOnBind)
    }

    /**
     * DrawingThread.getMapStatus() 方法详细说明
     *
     * 【方法功能】
     * 获取并更新地图状态信息（地图角度和车辆方向），用于控制地图模式按钮的显示
     *
     * 【调用时机】
     * - 通过 Handler 消息 MSG_RUN_GET_MAP_STATUS (值为2) 触发
     * - 默认每150ms调用一次
     * - 当角度变化超过5度时，会持续调用直到角度稳定
     *
     * 【关键变量解析】
     *
     * 1. mMapAngle (地图旋转角度)
     *    - 含义：地图相对于正北方的旋转角度（度）
     *    - 获取方式：this.mMapViewWrapper.getMapAngle()
     *                -> OperatorPosture.getRollAngle()
     *                -> OperatorPostureImpl.getRollAngle()
     *                -> Native层 getRollAngleNative()
     *    - 用途：旋转指北针（NorthDotSprite），使其始终指向北方
     *    - 更新时机：当变化超过5度时
     *
     * 2. mCarDir (车辆行驶方向)
     *    - 含义：车辆当前的行驶方向角度（度），即车头朝向
     *    - 取值范围：0-360度
     *      * 0° = 正北方向
     *      * 90° = 正东方向
     *      * 180° = 正南方向
     *      * 270° = 正西方向
     *    - 获取方式：从车辆位置信息中获取
     *      CarLoc carLocation -> vecPathMatchInfo[0].carDir
     *    - 获取条件：仅在正北向上模式（DrawModeType.NORTHUP）时获取
     *      其他模式（HEADUP_2D/HEADUP_3D）时，carLocation为null，mCarDir保持为0.0f
     *    - 用途：旋转箭头（ArrowSprite），使其始终指向车辆行驶方向
     *    - 更新时机：当变化超过5度时
     *
     * 3. isNeedUpdate(float oldValue, float newValue)
     *    - 作用：判断两个角度值的变化是否超过阈值
     *    - 阈值：5度（Math.abs(new - old) > 5.0f）
     *    - 目的：避免微小的角度变化导致频繁刷新绘制
     *
     * 【Hook 建议】
     *
     * Hook 点1：获取地图角度变化
     * - Hook: MapViewWrapper.getMapAngle()
     * - 位置：方法返回前
     * - 可获得：实时地图旋转角度
     *
     * Hook 点2：获取车辆方向变化（仅在NORTHUP模式）
     * - Hook: DrawingThread.getMapStatus()
     * - 位置：第285行 this.mCarDir = f; 之前或之后
     * - 可获得：车辆行驶方向角度
     *
     * Hook 点3：同时获取地图角度和车辆方向
     * - Hook: DrawingThread.getMapStatus()
     * - 位置：方法返回前（第289行之后）
     * - 可获得：完整的地图状态信息
     */
    private val mHookMethodDrawingThreadGetMapStatus: XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            try {
                afterHookedMethodCatching(param)
            } catch (t: Throwable) {
                Log.e(TAG, "HookMethodDrawingThreadGetMapStatus: ", t)
            }
        }

        private fun afterHookedMethodCatching(param: MethodHookParam) {
            val result: Any = param.result
            if (result !is Boolean) {
                return
            }
            if (result.not()) {
                return
            }

            val thisReflect = Reflect.on(param.thisObject)

            /**
             * 地图旋转角度
             * 控制指北针（N）的旋转，表示地图相对于北方的旋转
             */
            val mMapAngle: Float = thisReflect.get("mMapAngle")

            /**
             * 车辆行驶方向（车头朝向）
             * 控制箭头的旋转，表示车辆朝向
             */
            val mCarDir: Float = thisReflect.get("mCarDir")

            /**
             * package com.xiaopeng.montecarlo.views.mapmode;
             * public enum DrawModeType {
             *     NORTHUP,
             *     HEADUP_2D,
             *     HEADUP_3D
             * }
             */
            val mMode: Int = when (thisReflect.get<Any>("mMode").toString()) {
                "NORTHUP"   -> 0
                "HEADUP_2D" -> 1
                "HEADUP_3D" -> 2
                else        -> -1
            }

            // mMapAngle=78.0, mCarDir=0.0   -> 当前朝东，有点偏北
            // 0=北 90=东 180=南 270=西

            val broadcastSize: Int = mMiniServiceBinder.remoteCallbackList.beginBroadcast()
            for (i in 0 until broadcastSize) {
                mMiniServiceBinder.remoteCallbackList.getBroadcastItem(i).onMapStatusChanged(mMapAngle, mCarDir, mMode)
            }
            mMiniServiceBinder.remoteCallbackList.finishBroadcast()
        }
    }

    private val mHookMethodMiniMapServiceOnBind: XC_MethodHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val intent: Intent = param.args[0] as Intent
            if (intent.action != ConstantApp.ACTION_MAP_STATUS_CHANGED) {
                return
            }
            param.result = mMiniServiceBinder
        }
    }

    private val mMiniServiceBinder = object : IXposedInterface.Stub() {
        val remoteCallbackList: RemoteCallbackList<IXposedCallback> = RemoteCallbackList()

        override fun registerListener(callback: IXposedCallback) {
            remoteCallbackList.register(callback)
        }

        override fun unregisterListener(callback: IXposedCallback) {
            remoteCallbackList.unregister(callback)
        }
    }

}
