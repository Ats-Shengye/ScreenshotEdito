# ScreenshotEditor

iOS風のスクリーンショット撮影・編集機能をAndroidネイティブUIで実装したアプリケーションです。

## 概要

iPhoneの「撮影→編集→保存/コピー/破棄」のワークフローをAndroidで再現しました。
撮影から編集、保存まで一連の流れをシームレスに処理し、ユーザーの利便性を重視した設計になっています。

## 主な機能

- **即座に撮影**: ハードウェアキー、通知、クイック設定タイルから瞬時にスクリーンショット撮影
- **編集機能**: 自由な範囲選択が可能なクロップツール
- **柔軟な保存オプション**: ギャラリー保存、クリップボードコピー、他アプリへの共有
- **プライバシー重視**: インターネット権限不要、全処理をローカルで完結

## 技術的特徴

### アーキテクチャ
- **言語**: Kotlin 100%
- **UI**: Android View System（Jetpack Compose非使用）
- **画面キャプチャ**: MediaProjection API
- **画像処理**: Bitmap API、Canvas
- **ストレージ**: MediaStore API（Scoped Storage対応）

### パフォーマンス最適化
- 起動から編集画面まで1.0秒以内（ハイエンド端末）
- メモリ効率的な画像処理（ストリーミング処理）
- バッテリー消費を抑える前景サービス設計

### セキュリティ対策
- インターネット権限なし（完全オフライン動作）
- FileProviderによる安全なファイル共有
- PendingIntentのImmutableフラグ設定
- クリップボード自動クリア機能

## 動作環境

- Android 11（API Level 30）以上
- MediaProjection対応端末

## プロジェクト構成

```
app/
├── capture/          # スクリーンショット撮影
│   ├── CaptureActivity.kt
│   ├── CaptureService.kt
│   └── ProjectionController.kt
├── ui/              # UI関連
│   ├── EditorActivity.kt
│   ├── CropView.kt      # カスタムビュー実装
│   └── SettingsActivity.kt
├── data/            # データ処理
│   ├── MediaStoreSaver.kt
│   ├── ClipboardShare.kt
│   └── TempCache.kt
├── notif/           # 通知管理
│   └── PersistentNotifier.kt
└── tile/            # クイック設定タイル
    └── ScreenshotTileService.kt
```

## 実装の工夫点

### 1. メモリ効率
大きな画像でもOOMを回避するため、ストリーム処理とBitmapのリサイクルを徹底

### 2. ユーザビリティ
- 撮影タイミングの設定（即時/遅延）
- 前回の保存アクションを記憶
- クリップボード自動クリアのタイマー設定

### 3. エラーハンドリング
- MediaProjection権限失効時の再取得フロー
- FLAG_SECURE画面の適切な処理
- リソースリークを防ぐ確実な解放処理

## テスト

### テスト環境
- **実機テスト済み端末**
  - TORQUE G06（Android 13、カスタムハードキー搭載）
  - Anbernic RG350M

### テスト項目
- 画面回転時の動作確認
- 3ボタン/ジェスチャーナビゲーション両対応
- 各種画面密度での表示確認
- メモリリーク検証（LeakCanary使用）
- 異なるアスペクト比での表示・撮影確認
