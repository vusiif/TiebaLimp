# <p align="center">TiebaLimp</p>
<p align="center">
    <a href="https://github.com/vusiif/TiebaLimp/actions/workflows/build.yml">
        <img alt="Build Status" src="https://github.com/vusiif/TiebaLimp/actions/workflows/build.yml/badge.svg?branch=master">
    </a>
    <a href="https://github.com/vusiif/TiebaLimp">
        <img alt="License" src="https://img.shields.io/badge/license-GPLv3-blue.svg">
    </a>
</p>

**TiebaLimp** 是一款基于 **Kotlin + Jetpack Compose** 构建的现代化、高性能、高颜值的非官方百度贴吧 Android 客户端（继承并演进自 Tieba Lite 4.0 分支）。

---

## 🌟 核心特性 (Features)

* **📖 帖内阅读进度记忆与续读胶囊**：离开帖子自动记录阅读进度；二次进入时智能弹出“上次读到第 N 楼”悬浮胶囊，一键平滑跳转。
* **📑 楼层书签与抽屉面板**：可在任意楼层菜单中一键添加自定义备注书签；抽屉面板统一归档，多楼层快速对比与跳转。
* **🛡️ 高级黑名单与多维过滤矩阵**：
  * 支持 **关键词**、**正则表达式**、**吧内等级门限**、**IP 归属地** 与 **用户 UID/用户名** 多重规则；
  * 支持规则一键 JSON 复制导出、粘贴导入与防重复校验；
  * 内置单趟循环零对象分配与长文本 ReDoS 保护。
* **🖼️ “只看图”画廊瀑布流**：双列自适应瀑布流快速浏览帖内所有高分辨率图片，支持楼层徽章标记、点击查看大图与一键定位至正文楼层。
* **🌌 极致 Edge-to-Edge 沉浸与纯黑 OLED**：
  * Android 14+ 预测性返回动画（Predictive Back Gesture）物理手势缩放与圆角律动；
  * 系统栏对比度强制遮罩阻断，手势条色彩无缝渗透；
  * AMOLED `#000000` 纯黑无光调色，极致省电。
* **🔒 隐私与数据安全加固**：
  * 阻断 DataStore 默认值反向覆盖持久化配置的读写竞态；
  * 排除系统云备份与设备跨机迁移对本地账号凭据数据库（`tblite.db`）的提取；
  * 脱敏 ViewModel / Logcat 敏感认证参数与发帖草稿。

---

## 🛠️ 构建与开发 (Build & Setup)

* **开发环境**：
  * JDK 17
  * Android SDK Build-Tools 35.0.0 (`minSdk = 21`, `targetSdk = 35`)
  * Gradle 8.14.5 + AGP 8.13.2 + Jetpack Compose

```bash
# 检查编译与类型检查
./gradlew compileDebugKotlin

# 运行本地单元测试
./gradlew testDebugUnitTest

# 打包 Debug APK
./gradlew assembleDebug
```

---

## 📜 声明与致谢 (Credits & Disclaimer)

* 本项目为非官方客户端，源码及构建产物仅供个人技术研究与学习交流使用，严禁用于任何商业用途。
* 感谢原项目 [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite) 的卓越设计与开源贡献。
* 友情链接与依赖协议项目：
  * [Starry-OvO/aiotieba](https://github.com/Starry-OvO/aiotieba)
  * [n0099/tbclient.protobuf](https://github.com/n0099/tbclient.protobuf)
