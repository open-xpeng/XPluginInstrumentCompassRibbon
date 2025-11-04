# 方位指示器 (Compass Ribbon)

一个用于小鹏汽车仪表盘的 Xposed 插件，在仪表盘中显示当前方向指示器。

## 功能特性

- 🧭 **方向指示**：显示当前朝向方向（北、东北、东、东南、南、西南、西、西北）
- 📐 **角度显示**：支持 0-360 度角度显示
- 🔴 **中心指示线**：红色中心线指示当前方向
- 📍 **角度刻度点**：每 9 度显示一个刻度点

## 预览

![预览图](docs/6eeffecf27611cf8e80d856254375725.png)

## 安装说明

1. **前置要求**
   - 已 Root 的 Android 设备
   - 已安装 Xposed Framework 或 LSPosed

2. **安装步骤**
   - 安装 APK 文件
   - 在 Xposed 管理器中启用 "方位指示器" 模块
   - 将作用域设置为目标应用（`com.xiaopeng.instrument` 或 `com.xiaopeng.montecarlo`）
   - 重启目标应用或设备

## 使用说明

安装并激活模块后，插件会自动在目标应用的仪表盘中显示方向指示器。指示器会根据当前方向自动更新显示。

## 开发工具

强制杀死目标应用进程（用于开发调试）：

```shell
ps -elf | grep -w 'com.xiaopeng.instrument' | grep -v 'grep' | awk '{print $2}' | xargs kill -9
ps -elf | grep -w 'com.xiaopeng.montecarlo' | grep -v 'grep' | awk '{print $2}' | xargs kill -9
```

## 注意事项

⚠️ **重要提示**：
- 本插件需要 Root 权限和 Xposed Framework
- 仅适用于小鹏汽车的仪表盘应用
- 使用前请确保已备份系统
- 不当使用可能导致应用崩溃或系统不稳定

## 版本信息

- **当前版本**：1.0.0

## 许可证

本项目仅供学习和研究使用。
