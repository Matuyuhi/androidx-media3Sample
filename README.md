# androidx Media3 Sample

AndroidX Media3ライブラリを使用したメディア再生アプリのサンプル実装です。ビデオ・オーディオの再生、再生キューの管理、履歴記録、バックグラウンド再生などの機能を実装しています。

## 概要

このプロジェクトは、AndroidX Media3 (ExoPlayer) の基本的な機能と、実用的なメディア再生アプリケーションに必要な機能を実装したサンプルアプリケーションです。

## 主な機能

- **メディア再生**: ExoPlayerを使用したビデオ・オーディオ再生
- **バックグラウンド再生**: `MediaLibraryService`を使用したバックグラウンド対応
- **再生キューの永続化**: アプリ終了後も再生状態を復元
- **再生履歴**: メディアの再生履歴を自動記録
- **メディアキャッシュ**: 512MBのローカルキャッシュで効率的なストリーミング
- **カスタムコマンド**: キューの操作（次に追加、移動、削除、クリア）
- **Jetpack Compose UI**: モダンなUIフレームワークを使用

## 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose
- **メディア再生**: AndroidX Media3 (ExoPlayer 1.8.0)
- **依存性注入**: Hilt (Dagger)
- **非同期処理**: Kotlin Coroutines + Flow
- **データ永続化**: SharedPreferences + Kotlinx Serialization
- **最小SDK**: Android 8.0 (API 26)
- **ターゲットSDK**: Android 15 (API 36)

## アーキテクチャ

このアプリケーションは以下のレイヤーで構成されています:

### 1. UI層 (`ui/`)
- `VideoSurface.kt`: Compose統合されたプレイヤービュー

### 2. Playback層 (`playback/`)
- `PlaybackService.kt`: メディア再生のバックグラウンドサービス
- `PlayerManager.kt`: プレイヤーの状態管理とキューの永続化
- `SessionCallback.kt`: Media Sessionのコールバック処理とカスタムコマンド
- `HistoryRecorder.kt`: 再生履歴の記録

### 3. Data層 (`data/`)
- `MediaCatalog.kt`: メディアカタログの管理
- `QueueRepository.kt`: 再生キューの保存・復元
- `HistoryRepository.kt`: 再生履歴の保存・取得

### 4. DI層 (`di/`)
- `PlaybackModule.kt`: ExoPlayer、キャッシュ、通知の依存性注入設定

## プロジェクト構成

```
app/src/main/java/com/matuyuhi/media3/sample/
├── MainActivity.kt              # メインアクティビティ
├── MainApplication.kt           # Hiltアプリケーションクラス
├── playback/
│   ├── PlaybackService.kt       # メディア再生サービス
│   ├── PlayerManager.kt         # プレイヤー管理
│   ├── SessionCallback.kt       # セッションコールバック
│   └── HistoryRecorder.kt       # 履歴記録
├── data/
│   ├── MediaCatalog.kt          # メディアカタログ
│   ├── QueueRepository.kt       # キュー永続化
│   └── HistoryRepository.kt     # 履歴永続化
├── ui/
│   └── VideoSurface.kt          # 動画表示UI
└── di/
    └── PlaybackModule.kt        # 依存性注入モジュール
```

## セットアップ

### 必要要件

- Android Studio Ladybug | 2025.1.1 以降
- JDK 11
- Android SDK API 26 以上

### ビルド手順

1. リポジトリをクローン
```bash
git clone https://github.com/Matuyuhi/androidx-media3Sample.git
cd androidx-media3Sample
```

2. Android Studioでプロジェクトを開く

3. Gradleの同期を実行

4. エミュレーターまたは実機でアプリを実行

## 使い方

### 基本的な使い方

1. アプリを起動すると、サンプルメディアの再生が自動的に開始されます
2. プレイヤーコントロールで再生・一時停止・スキップなどが可能です
3. アプリを終了しても、次回起動時に再生位置とキューが復元されます

### カスタマイズ

#### メディアカタログの変更

`MediaCatalog.kt`でメディアアイテムを追加・変更できます:

```kotlin
MediaItem.Builder()
    .setMediaId("your_media_id")
    .setUri("https://your-media-url.mp4")
    .setMimeType(MimeTypes.VIDEO_MP4)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle("タイトル")
            .setArtist("アーティスト")
            .setArtworkUri("https://artwork-url.jpg".toUri())
            .build()
    )
    .build()
```

#### キャッシュサイズの変更

`PlaybackModule.kt`のキャッシュサイズを変更できます:

```kotlin
val evictor = LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024L) // 512MB
```

#### 履歴記録の閾値変更

`HistoryRecorder.kt`で履歴記録の閾値を変更できます:

```kotlin
if (playDuration >= 15000 || reason == HistoryRepository.CompletionReason.COMPLETED) {
    // 15秒以上再生で記録
}
```

## 主要コンポーネントの説明

### PlayerManager

- プレイヤーの初期化と状態管理
- 再生キューの保存・復元
- AudioFocus処理
- ヘッドフォン抜き差し対応

### SessionCallback

- Media Session接続管理
- カスタムコマンド処理:
  - `ADD_NEXT`: 現在の曲の次に追加
  - `MOVE_ITEM`: キュー内でアイテムを移動
  - `REMOVE_ITEM_AT`: 指定位置のアイテムを削除
  - `CLEAR_QUEUE`: キューをクリア
- メディアアイテムの解決

### HistoryRecorder

- 再生開始・終了の自動検出
- 15秒以上再生したメディアを履歴に記録
- 完走・スキップ・エラーの理由を記録
- 最大1000件の履歴を保持

### QueueRepository

- 再生キューの永続化
- シャッフル・リピートモードの保存
- 再生位置の保存

## 開発方針

### クリーンアーキテクチャ

- UI層、ビジネスロジック層、データ層を明確に分離
- 依存性注入で疎結合を実現
- テスタビリティを考慮した設計

### ベストプラクティス

- **Kotlin Coroutines**: すべての非同期処理はCoroutinesで実装
- **StateFlow**: UI状態の管理にStateFlowを使用
- **Hilt**: 依存性注入でシングルトンとスコープを管理
- **Media3**: 最新のAndroidX Media3 APIを活用
- **エラーハンドリング**: 適切なエラー処理と履歴記録

### 注意事項

- ExoPlayerは`release()`を呼ぶまでリソースを保持するため、適切なライフサイクル管理が重要
- バックグラウンド再生にはフォアグラウンドサービスとメディア通知が必須
- キャッシュサイズはデバイスのストレージ容量を考慮して設定

## ライセンス

このプロジェクトはサンプルコードです。自由に使用・改変できます。

## 参考リンク

- [AndroidX Media3 公式ドキュメント](https://developer.android.com/guide/topics/media/media3)
- [ExoPlayer ガイド](https://developer.android.com/guide/topics/media/exoplayer)
- [Media Session ガイド](https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
