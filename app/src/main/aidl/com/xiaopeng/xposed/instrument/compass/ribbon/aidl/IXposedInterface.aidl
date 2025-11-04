// IXposedInterface.aidl
package com.xiaopeng.xposed.instrument.compass.ribbon.aidl;

import com.xiaopeng.xposed.instrument.compass.ribbon.aidl.IXposedCallback;

interface IXposedInterface {
    void registerListener(IXposedCallback callback);
    void unregisterListener(IXposedCallback callback);
}
