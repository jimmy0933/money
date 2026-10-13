# 記帳與借貸管家 (MoneyTracker Android App)

一款 Android 原生 App，提供個人記帳、多帳戶管理、收支統計、借貸及分次還款管理，以及 Google Drive 雲端加密備份與還原。

## 一、系統特色與技術架構

* **程式語言 (Language)**: Kotlin
* **UI 框架**: Jetpack Compose + Material 3 (繁體中文介面)
* **架構設計**: MVVM + Repository (Offline First 設計)
* **本機資料庫**: Room Database (使用 Long 儲存 TWD 金額最小單位，避免浮點誤差)
* **背景任務**: WorkManager (定期與連線自動備份)
* **備份與還原**: Google Drive API (`appDataFolder` 專屬目錄)、PBKDF2 + AES 加密保護、SHA-256 Checksum 校驗碼
* **最低 Android 版本**: Android 10 (API 29)

---

## 二、功能說明

### 1. 記帳與多帳戶管理
* 支援「收入」、「支出」與「帳戶間轉帳」。
* 預設多個帳戶 (現金、銀行帳戶、電子支付、信用卡)，可隨時新增與編輯。
* 帳戶間轉帳不計入一般收支。
* 內建數字小鍵盤與分類篩選、關鍵字搜尋功能。

### 2. 借貸與分次還款管理
* 完整支援「我借給別人 (借出)」與「我向別人借 (借入)」。
* 自動計算原始借款、累計已還與剩餘欠款。
* 同一個人支援多筆借貸，可同時存在借入與借出（分開呈現，不自動抵銷）。
* **還款保護機制**: 限制累積還款金額不得超過借貸款項本金。
* 借貸本金與還款金流自動反映至指定帳戶餘額，但標記隔離以防止失真一般收支報表。

### 3. 雲端備份與還原
* 支援將本機資料備份至 Google Drive 專屬 `appDataFolder`。
* 支援可選密碼加密備份。
* **交易式還原保護**: 還原過程採 Room Transaction，若解密或還原失敗不會損壞本機資料。

---

## 三、Google OAuth Client ID 與 Drive API 設定指南

* **Android Package Name**: `com.moneytracker.app`

在實際實機環境啟用 Google 帳號登入與 Google Drive 雲端備份時，請依照下列步驟於 Google Cloud Console 與 GitHub 設定：

1. **開啟 Google Cloud Console 專案與啟用 Drive API**:
   * 造訪 [Google Cloud Console](https://console.cloud.google.com/) 並建立或選擇專案。
   * 進入「API 與服務」 > 「程式庫」，搜尋 `Google Drive API` 並點擊「啟用 (Enable)」。

2. **設定 OAuth 同意畫面與測試使用者**:
   * 進入「API 與服務」 > 「OAuth 同意畫面」。
   * 選擇「外部 (External)」，填寫應用程式名稱與支援 Email。
   * 新增必要的 Scope 權限範圍：`https://www.googleapis.com/auth/drive.appdata` 與 `https://www.googleapis.com/auth/drive.file`。
   * 在「測試使用者 (Test users)」清單中新增進行測試的 Google 帳號。

3. **提取 APK 簽章 SHA-1 / SHA-256 指紋**:
   * **方式 A (從本地 Gradle 簽章報告提取)**:
     ```bash
     ./gradlew signingReport
     ```
   * **方式 B (從已產出的 APK 檔案直接提取簽章指紋)**:
     ```bash
     keytool -list -printcert -jarfile app-debug.apk
     ```
     *(注意：請勿將「APK 檔案 SHA-256 校驗碼」誤當成「APK 簽章證書 SHA-1 指紋」！)*

4. **建立 Android OAuth 2.0 Client ID**:
   * 進入 Google Cloud Console「憑證 (Credentials)」 > 「建立憑證」 > 「OAuth 用戶端 ID」。
   * **應用程式類型**: 選擇「Android」。
   * **Package Name**: `com.moneytracker.app`
   * **SHA-1 憑證指紋**: 填入上述步驟取得之 SHA-1 簽章指紋。

5. **設定 GitHub Secrets (專用 Release 簽章庫)**:
   * 若要在 CI/CD (GitHub Actions) 產出具備固定簽章的測試 APK，請於 GitHub Repository > Settings > Secrets and variables > Actions 新增以下Secrets：
     * `KEYSTORE_BASE64`: keystore 檔案之 Base64 編碼字串
     * `KEYSTORE_PASSWORD`: keystore 密碼
     * `KEY_ALIAS`: key 別名
     * `KEY_PASSWORD`: key 密碼

> ⚠️ **重要說明 (真實帳號驗證狀態聲明)**:
> 未在 Google Cloud Console 上完成此 Android 包名 (`com.moneytracker.app`) 與 SHA-1 簽章指紋配置前，Google Play 服務會拒絕 OAuth 請求（通常拋出 `ApiException: 10 DEVELOPER_ERROR` 或 `12500`）。在缺乏實際 Google Cloud 憑證配置的環境下，**實機真實 Google 帳號登入列為待完成驗證**，本 App 已提供完整的錯誤碼揭露、診斷日誌及權限補全導引。

---

## 四、編譯與執行指令

### 執行單元測試 (Unit Tests)
在連結網際網路且能下載完整 Maven 依賴庫之環境下，執行以下指令：
```bash
./gradlew test
```

### 建置 Debug APK
在連結網際網路且能下載完整 Maven 依賴庫之環境下，執行以下指令：
```bash
./gradlew assembleDebug
```
產出的 APK 檔案將位於：
`app/build/outputs/apk/debug/app-debug.apk`

*(註：若在完全無外網存取的隔離 Sandbox 環境中，外部 Maven 儲存庫 `com.google.apis:google-api-services-drive` 依賴庫之 Jar 檔將無法下載；請確保在連網環境下建置即可成功產出 APK。)*

---

## 五、GitHub Actions CI/CD 與 APK 下載

本專案配置有 GitHub Actions 工作流程 `.github/workflows/build-apk.yml`：

### 1. 自動觸發機制
* **PR 自動驗證**: 當有 Pull Request 指向 `master` 分支時，自動觸發單元測試與 APK 建置。
* **合併/推送自動建置**: 當程式碼合併或直接推送到 `master` 分支時，自動建置並產出最新 APK。

### 2. 手動觸發建置 (`workflow_dispatch`)
1. 前往專案的 GitHub Repository 頁面，點選 **Actions** 頁籤。
2. 在左側工作流程清單中選擇 **Build & Package Android APK**。
3. 點擊右側的 **Run workflow** 按鈕，選擇分支並點擊確認執行。

### 3. 下載 APK Artifacts、校驗碼與測試報告
1. 進入任何一次執行的 Action Run 詳情頁面。
2. 捲動至頁面底部的 **Artifacts** 區塊：
   * **`app-debug-apk-<commit-sha>`**: 包含檔名標示有 commit 短碼之 Debug APK（如 `app-debug-a1b2c3d.apk`）與 SHA-256 校驗碼檔案（如 `app-debug-a1b2c3d.apk.sha256`）。
   * **`test-report-<commit-sha>`**: 包含單元測試報告（HTML / XML 格式）。
3. Artifacts 保留期限為 30 天。若測試失敗，產物仍會保留備查與下載。

---

## 六、預覽與畫面截圖

* **首頁 (Home)**: 收支總覽、帳戶餘額、待收/待還金額與最近交易紀錄。
* **記帳頁 (Transactions)**: 交易明細、月份/分類/關鍵字篩選與快速記帳數字鍵盤。
* **借貸頁 (Loans)**: 借出/借入/對象總覽 Tab，卡片式狀態顯示與分次還款對話框。
* **統計頁 (Statistics)**: 支出分類圓餅圖與收支長條圖比較。
* **設定頁 (Settings)**: Google Drive 連結、立即備份、雲端還原與分類/帳戶管理。
