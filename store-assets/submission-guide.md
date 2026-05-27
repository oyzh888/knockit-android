# Google Play Console 提交指南 — Knockit

## 第一步：构建 Release AAB

### 方案A：通过 GitHub Actions（推荐，本地无需 Android SDK）

1. 把代码推送到 GitHub 新仓库（或推到 oyzh888/knockit 的 android 分支）
2. 在 GitHub 仓库设置以下 Secrets（Settings → Secrets → Actions）：
   ```
   KEYSTORE_BASE64   = (见下方命令获取)
   KEYSTORE_PASSWORD = knockit2024
   KEY_ALIAS         = knockit
   KEY_PASSWORD      = knockit2024
   ```
3. 获取 keystore base64：
   ```bash
   base64 -w 0 /workspace/knockit-android/app/keystore/knockit-release.jks
   ```
4. 推送 tag 触发构建：
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
5. 在 Actions 页面下载 `knockit-release-aab` artifact（`app-release.aab`）

### 方案B：本地构建（需要 Android Studio）
```bash
cd /workspace/knockit-android
./gradlew bundleRelease
# 输出：app/build/outputs/bundle/release/app-release.aab
```

---

## 第二步：托管隐私政策

最简单：GitHub Pages
1. 新建 GitHub 仓库 `knockit-privacy`
2. 上传 `store-assets/privacy-policy.html` 改名为 `index.html`
3. Settings → Pages → Source: main → Save
4. URL: `https://你的用户名.github.io/knockit-privacy`
5. 等约 2 分钟生效

---

## 第三步：Google Play Console 提交步骤

### 进入 Console
访问：https://play.google.com/console

### 创建新 App
1. 点击 "Create app"
2. App name: `Knockit - AI Smart Reminders`
3. Default language: English (United States)
4. Free
5. 接受政策 → Create app

### 填写 Store listing（从 listing.md 复制）
路径：Grow → Store presence → Main store listing
- App name / Short description / Full description
- 上传 App icon（512×512 PNG）
- 上传 Feature Graphic（1024×500 PNG）
- 上传手机截图（至少2张，建议4张，1080×1920 PNG）
- Category: Productivity
- Contact email
- Privacy policy URL（第二步获取的 URL）

### 完成政策问卷

**App Access**（Policy → App content → App access）
- All functionality is available without special access

**Ads**
- No ads

**Content Rating**（完成问卷）
- Category: Utility
- 所有问题选 No → 预期结果: Everyone (E)

**Target Audience**
- Age: 13 and over
- Not targeting children: No

**Data Safety**（Policy → App content → Data safety）
声明以下数据：
| 数据类型 | 收集 | 共享 | 用途 |
|---------|------|------|------|
| 近似位置 | 是（可选） | 否 | 功能（祈祷时间） |
无其他个人数据，数据不出售

### 上传 AAB
路径：Release → Production → Create new release
1. 上传 `app-release.aab`
2. Release name: `1.0.0`
3. Release notes：粘贴 listing.md 的 What's New 内容

### 发布
1. Review release → 解决所有警告
2. Start rollout to Production → 确认

---

## 审核时间
- 首次发布通常需要 **3-7 天**
- 会收到邮件通知结果
- 期间可在 Play Console 查看状态

---

## Keystore 备份（重要！！！）
```
文件位置：/workspace/knockit-android/app/keystore/knockit-release.jks
密码：    knockit2024
Key别名：  knockit
Key密码：  knockit2024

⚠️  请立即把这个文件备份到安全位置（不能丢失！丢失后无法更新App）
建议备份到：
  - /workspace/ 目录（Windows 主机持久化）
  - 个人网盘（加密存储）
```

---

## 图标生成（快速方案）
访问 https://icon.kitchen（免费）
- Background color: #1C1C2E
- 图标内容：bell emoji 或上传 bell SVG
- 下载 → 选取 512×512 PNG 用于 Play Store

## 截图拍摄
使用 Android 模拟器（Android Studio 内置）：
1. 创建 Pixel 7 模拟器（API 33）
2. 安装 debug APK：`./gradlew installDebug`
3. 截图保存（模拟器相机图标）
4. 尺寸需要 1080×2340 或 1080×1920
