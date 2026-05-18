# Android 互动动态壁纸 MVP

## 已实现
- 本地选择 `video/*`，并持久化 URI 权限（`takePersistableUriPermission`）。
- DataStore 保存视频 URI、静音、填充模式、播放速度、触摸特效、视差。
- Compose 三页：首页、预览页、设置页。
- 动态壁纸 `WallpaperService` + `Engine` 生命周期管理。
- 使用 Media3 ExoPlayer 循环播放本地视频，默认静音。
- 填充模式枚举：`CENTER_CROP` / `FIT_CENTER` / `STRETCH`（当前 MVP 先保存并透传，后续可继续细化到实际矩阵变换）。
- 触摸波纹特效（自动淡出和移除）。
- 桌面偏移视差（在叠加层做轻微平移）。
- 无视频时展示深色占位与提示，不崩溃。

## 未实现（刻意留到后续版本）
- 不实现 iOS Nugget / PosterBoard / MobileGestalt / SpringBoard / sparserestore / BookRestore。
- 不实现 root / Shizuku / ADB / 无障碍 / 悬浮窗权限能力。
- 不实现广告、账号、云同步、商店。
- 不承诺所有品牌锁屏都支持强交互（系统与厂商限制）。

## 运行命令
```bash
./gradlew assembleDebug
./gradlew test
```

## 手动测试步骤
1. 安装 debug 包并启动 App。
2. 点击“选择视频”，选择本地 MP4（`video/*`）。
3. 关闭重开 App，确认首页仍显示已选 URI。
4. 点击“预览壁纸”，确认可循环播放与触摸波纹。
5. 点击“设置为动态壁纸”，进入系统动态壁纸页面并应用。
6. 回到桌面观察视频循环播放。
7. 切到其他 App 后返回桌面，确认壁纸继续正常。
8. 设置页修改静音、速度、触摸和视差后再次验证。

## 说明
- 可预留 `TendiesExtractor` 接口，但本版不实现实际提取逻辑。
