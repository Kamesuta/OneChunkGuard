# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## コマンド (Commands)

### ビルド・開発 (Build & Development)
```bash
mvn clean package    # プラグインをビルドしてJARファイルを生成
mvn clean install    # 依存関係を含めてビルド（現在はtests無し）
```

## アーキテクチャ概要 (Architecture Overview)

OneChunkGuardは、プレイヤーが1チャンクのみ保護可能なMinecraft Spigot/Paperプラグインです。WorldGuard APIを使用して土地保護を実装しています。

### コア設計原理 (Core Design Principles)

#### 1. 複数保護ブロック種類対応
- `protection-blocks`設定で複数の保護ブロック種類を定義可能
- 各種類は独立した保護を持ち、プレイヤーは異なる種類なら複数の保護が可能
- WorldGuardリージョン名: `onechunk_{種類}_{プレイヤーUUID}`

#### 2. 親リージョン制限システム
- 特定の保護ブロック種類は指定された親リージョン内でのみ設置可能
- `parent-region`設定で制限を定義

#### 3. 可変チャンク範囲
- `chunk-range`設定で保護範囲を指定（1=1x1、3=3x3など）
- 中央チャンクを基準に範囲を展開

### 主要コンポーネント (Main Components)

#### Managers（管理クラス）
- **ProtectionManager**: WorldGuard APIとの連携、保護の作成・削除
- **DataManager**: 保護データの永続化、YAML管理
- **ConfigManager**: 設定ファイルの管理、複数保護ブロック種類の定義

#### Models（データモデル）
- **ProtectionData**: 保護情報（所有者、位置、信頼プレイヤー、種類）
- **ProtectionBlockType**: 保護ブロック種類の定義（素材、範囲、親リージョン）

#### Listeners（イベント処理）
- **ProtectionBlockInventoryListener**: スロット9固定機能（defaultのみ）
- **PlayerJoinListener**: 初回ログイン時の保護ブロック配布
- **ProtectionBlockInteractListener**: 右クリックによるチャットTUI
- **ProtectionBlockPlaceBreakListener**: 設置・破壊処理
- **ChunkEntryListener**: チャンク入場時の所有者表示
- **ChunkVisualizerListener**: パーティクル表示によるチャンクビジュアライザー

### 重要な実装詳細 (Important Implementation Details)

#### 保護ブロックの種類別処理
- **defaultブロック**: スロット9に固定、常に返却される
- **非defaultブロック**: 通常アイテム扱い、実際に破壊された場合のみ返却

#### データ管理の二重構造
- `playerProtections`: UUID→ProtectionData（後方互換性）
- `playerTypeProtections`: "UUID:種類"→ProtectionData（複数種類対応）

#### セレクター対応
- `Bukkit.selectEntities()`を使用して@p、@r等のセレクター対応
- trust/untrust/giveprotectionblockコマンドで利用可能

## 設定システム (Configuration System)

### 保護ブロック種類設定
```yaml
protection-blocks:
  default:
    material: END_STONE
    display-name: "&6&l保護ブロック"
    parent-region: ""      # 制限なし
    chunk-range: 1         # 1x1チャンク
  vip:
    material: DIAMOND_BLOCK
    display-name: "&b&lVIP保護ブロック"
    parent-region: "vip_area"  # vip_area内でのみ設置可能
    chunk-range: 3         # 3x3チャンク
```

## パッケージ構造とJavaバージョン (Package Structure & Java Version)
- パッケージ: `com.kamesuta.onechunkguard`
- Java 21必須（Paper API 1.21対応）
- 依存関係: WorldGuard 7.0+、WorldEdit 7.2+

## 開発時の注意点 (Development Notes)
- すべてのメッセージは日本語で`config.yml`のmessagesセクションに定義
- NBTタグを使用した保護ブロック識別: `onechunkguard:type`
- WorldGuardリージョンの親子関係を考慮した重複チェック
- 遠隔地からの保護ブロック破壊検証ロジック

## 開発サイクル (Development Cycle)

このプロジェクトでは以下のサイクルで開発を進めています：

### 1. 仕様策定 → 2. TODO管理 → 3. 実装 → 4. 仕様反映

#### ファイルの役割
- **`Spec.md`**: プラグインの仕様書（完成形の設計図）
- **`TODO.md`**: 実装タスクの管理（[ ]/[x]でチェックボックス管理）
- **`CLAUDE.md`**: 開発ガイダンス（本ファイル）

#### 開発フロー
1. **新機能・改善案** → `Spec.md`に追加
2. **実装タスク洗い出し** → `TODO.md`にチェックボックス形式で追加
3. **実装作業** → コードを書きながら`TODO.md`の項目を[x]に更新
4. **完了時** → `Spec.md`の「実装完了機能」セクションに✅付きで追加

#### 重要な原則
- **TODO.mdの更新**: 実装完了と同時に必ず[x]をつける
- **Spec.mdへの反映**: 機能完了後は仕様書に実装済みとして記録
- **段階的開発**: 大きな機能は小さなTODOに分割して管理
- **後方互換性**: 既存機能を壊さないよう注意深く実装

この3つのファイルを連携させることで、仕様→実装→検証のサイクルを効率的に回しています。