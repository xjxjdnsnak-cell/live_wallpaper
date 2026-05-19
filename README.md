# Android 互动动态壁纸 MVP

这是一个 Kotlin Android 动态壁纸 MVP。当前目标是先保证本地视频能作为系统动态壁纸稳定循环播放，再逐步扩展交互效果。

## 已实现

- 选择本地 `video/*` 文件，并通过 `takePersistableUriPermission` 持久化读取权限。
- 使用 DataStore 保存视频 URI、静音、填充模式、播放速度、触摸特效开关、视差开关等基础设置。
- 提供 Jetpack Compose 首页、预览页、设置页。
- 通过 `WallpaperService` 注册系统动态壁纸入口。
- 使用 Media3 ExoPlayer 在壁纸 Surface 上循环播放本地视频，默认静音。
- 点击“设置为动态壁纸”进入系统动态壁纸设置流程。
- 预览页支持视频播放和触摸波纹演示。
- 无视频 URI 时，真实壁纸 Surface 显示占位背景和提示文字，不应崩溃。

## 实验功能与限制

- 真实系统壁纸中的触摸波纹暂不保证稳定。当前版本为了避免 Canvas 与 ExoPlayer 争用同一个 Surface，有视频时优先保证视频播放，不在真实壁纸 Surface 上叠画波纹。
- 预览页的触摸波纹只是 Compose 页面内的演示效果，不代表所有桌面或锁屏环境都能强交互。
- 锁屏动态壁纸和锁屏强交互受 Android 系统与厂商实现限制，本项目不保证所有设备都支持。
- 填充模式第一版只做基础映射：预览页使用 `PlayerView` 的 resize mode；真实壁纸使用 ExoPlayer 的 video scaling mode，`STRETCH` 在真实壁纸中可能只部分生效。
- 桌面视差目前只影响无视频占位/实验绘制层，不影响 ExoPlayer 视频帧。

## 未实现

- 不实现 iOS Nugget / PosterBoard / MobileGestalt / SpringBoard / sparserestore / BookRestore。
- 不实现 `.tendies` 完整兼容或真实导入转换流程；仓库中预留的 `TendiesExtractor` 只是后续扩展接口。
- 不实现 root / Shizuku / ADB / 无障碍 / 悬浮窗权限能力。
- 不实现壁纸市场、广告、账号、云同步。

## 运行命令

```bash
./gradlew assembleDebug
./gradlew test
```

Windows PowerShell 环境可使用：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

## 手动测试步骤

1. 安装 debug 包并启动 App。
2. 点击“选择视频”，选择本地 MP4 或其他系统可解码的 `video/*` 文件。
3. 关闭并重开 App，确认仍能显示已保存的视频 URI。
4. 进入“预览壁纸”，确认视频能循环播放，点击预览区域能看到触摸波纹。
5. 返回首页，点击“设置为动态壁纸”，确认能进入系统动态壁纸设置页。
6. 应用壁纸后回到桌面，确认视频能循环播放。
7. 切到其他 App 后再回桌面，确认壁纸没有崩溃，回到桌面后继续播放。
8. 在设置页修改静音、播放速度、触摸特效、视差和填充模式，再回到桌面确认播放器设置能同步。
9. 清空或未选择视频时设置动态壁纸，确认显示占位提示且不崩溃。
