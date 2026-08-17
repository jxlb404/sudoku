# 数独 · Android 版

一个用 **Kotlin + Jetpack Compose** 编写的原生安卓数独应用，界面清爽现代，支持深浅色主题。

## 功能

- 三档难度：**简单 / 普通 / 专家**（专家提示数 24–28，并按解题搜索工作量筛选高难题），每道题**保证唯一解**，题目呈 180° 对称
- **每日一题**：按日期固定生成，当天不换题
- **推演模式**：封存当前局面（灰色滤镜），可自由试填、不计错误；「✓ 应用」把模拟结果覆盖到真实盘面，「退出」则作废模拟
- **报错方式可选**：⚡ 及时报错 / 🏁 完成后检查（全部填完后自动核对，有错才标红）
- **高对比界面**：背景 / 棋盘 / 数字三层层级分明，键盘深色配色统一
- **超数独 / X 数独**：支持四窗 Windoku 与双对角线玩法，功能与标准数独完全一致（含每日一题、独立排行榜等），难度优化统一
- **本地排行榜**：设置默认用户名，按模式独立排名（仅显示前十），个人最佳 / 最近 / 平均用时对比（纯本地保存）
- **自选提示**：先点选空格子再点提示，不按顺序自动填
- **进度存档**：退出时弹窗选择「保存并退出 / 不保存退出 / 继续游戏」；主页可继续上次游戏或舍弃存档
- **完成记录**：主页保留最近一次通关（难度、用时、错误、日期），可查看棋盘或重玩同一题
- 计时、错误计数、撤销、擦除、笔记（候选数）、提示
- 选中联动高亮（同行/同列/同宫、相同数字）、冲突自动标红
- 完成时弹出成绩（用时、错误次数），可一键再来一局

## 一、用 Android Studio 安装到手机

1. 安装 [Android Studio](https://developer.android.com/studio)（下载约 1~2 GB），首次启动时把 SDK 装好。
2. 打开 Android Studio，选择 **File → Open…**，选中本文件夹（`sudoku-android`）。
3. 首次打开会自动下载 Gradle 8.10.2 和依赖库（需要联网，首次可能要几分钟）。
4. 手机开启「开发者选项 → USB 调试」，用数据线连接电脑，手机上允许调试。
5. 点击工具栏绿色 ▶（Run），选择你的手机，App 会自动安装并启动。

## 二、直接生成 APK 手动安装

1. 菜单 **Build → Build App Bundle(s) / APK(s) → Build APK(s)**。
2. 完成后 APK 在 `app/build/outputs/apk/debug/app-debug.apk`。
3. 把这个 APK 文件传到手机（微信文件传输助手、QQ、数据线均可），点击安装。
4. 如果提示「未知来源」，在设置里允许安装该应用即可。

## 三、建议：把工程放到非 C 盘

- 整个文件夹建议放到 `G:\SudokuApp` 这类位置，再打开工程。
- 设置用户环境变量 `GRADLE_USER_HOME=G:\Android\Gradle`，构建缓存就不占 C 盘。
- 如果用模拟器，设置 `ANDROID_AVD_HOME=G:\Android\avd`，虚拟手机会放到非 C 盘。

## 四、常见问题

- **国内下载 Gradle/依赖慢**：可以在 `gradle/wrapper/gradle-wrapper.properties` 里把
  `distributionUrl` 换成国内镜像地址（例如腾讯云镜像），或配置阿里云 Maven 仓库。
- **提示 Gradle wrapper 缺失**：Android Studio 会自动尝试生成 wrapper；也可以从任意
  其他 Android 工程的 `gradle/wrapper/` 目录复制 `gradle-wrapper.jar` 到本工程的同名目录。
- **手机要求**：Android 8.0（API 26）及以上，绝大多数手机都满足。

## 目录结构

```
sudoku-android/
├── app/src/main/java/com/nendie/sudoku/
│   ├── MainActivity.kt        # 入口
│   ├── SudokuEngine.kt        # 数独生成 / 唯一解校验算法
│   ├── GameViewModel.kt       # 游戏状态、计时、撤销栈
│   └── ui/
│       ├── Theme.kt           # 深浅色主题
│       └── SudokuScreen.kt    # 主页 + 游戏界面（Compose）
└── app/src/main/res/          # 图标、字符串、主题资源
```
