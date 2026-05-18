# Android 互动动态壁纸 MVP

## 已实现（当前版本）
- 本地选择 `video/*`（OpenDocument）并持久化 URI 读取权限。
- DataStore 保存：视频 URI、静音、填充模式、播放速度、触摸特效开关、视差开关。
- 首页/预览页/设置页（Jetpack Compose）。
- 跳转系统动态壁纸设置并应用当前 Live Wallpaper Service。
- 壁纸服务读取本地 URI 后使用 Media3 ExoPlayer 循环播放。
- 壁纸无视频 URI 时显示深色占位和提示文本（不崩溃）。
- 预览页支持触摸波纹演示。

## 实验功能与限制
- **真实壁纸上的触摸波纹**：当前版本保留触摸数据逻辑，但为了稳定播放，未在 ExoPlayer 正在使用的同一 Surface 上强行叠加 Canvas 波纹。
- **锁屏交互**：不同厂商和系统策略差异较大，不保证所有机型锁屏强交互一致可用。
- **填充模式**：第一版已保存并可配置，实际视频裁剪/矩阵细节为后续可继续增强项。

## 未实现（本版明确不做）
- iOS Nugget / PosterBoard / MobileGestalt / SpringBoard / sparserestore / BookRestore。
- `.tendies` 完整兼容（仅预留 `TendiesExtractor` 接口）。
- 壁纸模板市场、账号体系、云同步、广告。
- root / Shizuku / ADB / 无障碍服务 / 悬浮窗权限方案。

## 运行命令
```bash
./gradlew assembleDebug
./gradlew test
```

## 手动测试步骤
1. 安装 Debug 包并启动 App。
2. 点击“选择视频”，选择本地 MP4（`video/*`）。
3. 关闭并重开 App，确认首页仍显示已保存 URI。
4. 点击“预览壁纸”，确认视频可播放，点击有波纹演示。
5. 点击“设置为动态壁纸”，进入系统页面并应用。
6. 回到桌面，确认视频循环播放。
7. 切到其他 App 再返回桌面，确认可恢复播放且无明显异常占用。
8. 清空/未设置视频时，应用壁纸后应看到占位提示，不崩溃。
