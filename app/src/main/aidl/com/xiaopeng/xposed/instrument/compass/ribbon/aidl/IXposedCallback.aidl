// IXposedCallback.aidl
package com.xiaopeng.xposed.instrument.compass.ribbon.aidl;

interface IXposedCallback {
    void onMapStatusChanged(float mapAngle, float carDir, int drawMode);
}
