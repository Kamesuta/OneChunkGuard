# OneChunkGuard

> シンプルな1チャンク保護プラグイン for Minecraft

OneChunkGuardは、各プレイヤーが1つのチャンクのみを保護できる、初心者にも優しいMinecraftプラグインです。WorldGuard APIを活用し、直感的で使いやすいインターフェースを提供します。

## ✨ 主な機能

### 🎮 初心者に優しい設計
- **コマンド不要**：保護ブロックを置くだけで自動保護
- **初回ログイン時に自動配布**：新規プレイヤーには保護ブロックを自動で配布
- **視覚的表示**：保護ブロックの上にプレイヤーの頭を表示

### 🛡️ 保護システム
- **1人1チャンク制限**：各プレイヤーは同時に1つのチャンクのみ保護可能
- **自動保護**：保護ブロックを設置するとチャンク全体が自動保護
- **簡単解除**：保護ブロックを破壊するか`/unprotect`コマンドで解除

### 👥 共同プレイヤー機能
- **信頼システム**：他のプレイヤーをチャンクに招待可能
- **チャットTUI**：保護ブロックを右クリックでメニューが表示
- **ワンクリック操作**：コマンドを簡単に入力できるボタン

### 🔒 セキュリティ
- **厳格な権限管理**：保護ブロックは所有者とOPのみ破壊可能
- **既存保護との競合回避**：WorldGuardの既存領域と重複しない
- **アイテム固定**：保護ブロックはホットバー9番目に固定、譲渡・ドロップ不可

## 📋 必要な環境

- **Minecraft**: 1.20以降
- **サーバー**: Spigot/Paper
- **依存プラグイン**: 
  - WorldGuard 7.0+
  - WorldEdit 7.2+

## 🚀 インストール方法

1. [Releases](https://github.com/kamesuta/OneChunkGuard/releases)から最新版をダウンロード
2. `plugins`フォルダに`OneChunkGuard-X.X.X.jar`を配置
3. サーバーを再起動
4. プラグインが正常に読み込まれたことを確認

## 🎯 使い方

### 基本的な使い方

1. **初回ログイン**：サーバーに初めて参加すると、ホットバー9番目に保護ブロックが配布されます
2. **チャンク保護**：保護ブロックを好きな場所に設置するとそのチャンクが保護されます
3. **保護解除**：保護ブロックを破壊するか`/unprotect`で保護を解除できます

### コマンド一覧

| コマンド | 説明 | 使用例 |
|---------|-----|--------|
| `/unprotect` | 保護を解除してブロックを返却 | `/unprotect` |
| `/trust <プレイヤー名>` | プレイヤーを信頼リストに追加 | `/trust Steve` |
| `/untrust <プレイヤー名>` | プレイヤーを信頼リストから削除 | `/untrust Steve` |
| `/trustlist` | 信頼プレイヤー一覧を表示 | `/trustlist` |

### チャットTUI の使い方

保護ブロックを右クリックすると、チャットにメニューが表示されます：

- **[メンバー追加]**：クリックで`/trust `コマンドが入力されます
- **[メンバー削除]**：クリックで`/untrust `コマンドが入力されます  
- **[メンバー一覧]**：クリックで信頼プレイヤー一覧が表示されます

## ⚙️ 設定

`plugins/OneChunkGuard/config.yml`で設定をカスタマイズできます：

```yaml
# 保護ブロックの設定
protection-block:
  material: END_STONE          # 保護ブロックの素材
  display-name: "&6&l保護ブロック"
  lore:
    - "&7このブロックを設置すると"
    - "&7チャンクが保護されます"
    - "&c1人1チャンクまで！"

# 保護設定
protection:
  min-y: -64                   # 保護する最低高度
  max-y: 320                   # 保護する最高高度
  max-trusted-players: 5       # 最大信頼プレイヤー数

# メッセージ設定（カスタマイズ可能）
messages:
  protection-created: "&a保護が有効になりました！"
  # ... その他のメッセージ
```

## 🔧 開発・ビルド

### 必要な環境
- Java 21以降
- Maven 3.8以降

### ビルド方法
```bash
git clone https://github.com/kamesuta/OneChunkGuard.git
cd OneChunkGuard
mvn clean package
```

ビルドされたJARファイルは`target/onechunkguard-X.X.X.jar`に生成されます。

## 🐛 バグ報告・機能要望

バグを発見した場合や新機能の要望がある場合は、[GitHub Issues](https://github.com/kamesuta/OneChunkGuard/issues)でお知らせください。

## 📜 ライセンス

このプロジェクトはMITライセンスの下で公開されています。詳細は[LICENSE](LICENSE)ファイルをご覧ください。

## 🙏 謝辞

- [WorldGuard](https://github.com/EngineHub/WorldGuard) - 土地保護システムの基盤
- [WorldEdit](https://github.com/EngineHub/WorldEdit) - WorldGuardの依存関係
- [Spigot/Paper](https://papermc.io/) - Minecraftサーバープラットフォーム
