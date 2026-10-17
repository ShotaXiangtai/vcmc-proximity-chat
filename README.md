# ProximityChat

Foliaに完全対応した、Paper/Foliaサーバー向けの近距離チャットプラグインです。
プレイヤー間の距離に応じてチャットの表示（接頭辞と色）が3段階に変化します。

## 概要 (Overview)
このプラグインはサーバー内のチャットを近距離チャットに制限します。
Kyori Adventure APIを活用してリッチなテキストコンポーネントを生成し、Foliaの `EntityScheduler` を利用することで、マルチスレッド環境下でも安全にプレイヤーのロケーションを計算し、チャットを配信します。
また、モダンなPaperの `LifecycleEventManager` (Brigadier) を用いたコマンド登録を実装しています。

## 機能 (Features)
- **距離に基づく3段階のチャット表示**:
  - `[とても近く]` (緑色): デフォルト 5ブロック以内
  - `[近く]` (黄色): デフォルト 15ブロック以内
  - `[周辺]` (灰色): デフォルト 25ブロック以内
- **Folia対応**: `GlobalRegionScheduler` と `EntityScheduler` を利用したスレッドセーフな実装。
- **動的設定変更**: コマンドからチャットの最大可視距離をリアルタイムに変更可能。
- **全体アナウンス**: 運営用のグローバルチャット機能（`/announce`）。

## コマンド (Commands)
| コマンド | 説明 | 権限 |
| --- | --- | --- |
| `/announce <メッセージ>` | サーバー全体にアナウンスを送信します | `proximitychat.announce` (OP) |
| `/setradius <数値>` | チャットの最大可視距離を変更します | `proximitychat.setradius` (OP) |
| `/pcstatus` | 現在の近距離チャット設定（距離のしきい値）を確認します | `proximitychat.status` (全員) |

## パーミッション (Permissions)
- `proximitychat.announce` : `/announce` コマンドの実行権限 (デフォルト: op)
- `proximitychat.setradius` : `/setradius` コマンドの実行権限 (デフォルト: op)
- `proximitychat.status` : `/pcstatus` コマンドの実行権限 (デフォルト: true)

## 設定 (Configuration)
`config.yml` にて以下の設定を変更できます。
```yaml
# 近距離チャット設定
very-close: 5    # 「とても近く」のしきい値（ブロック）
close: 15        # 「近く」のしきい値（ブロック）
max-radius: 25   # 最大可視距離（ブロック）。/setradius で変更可能
```

## コード構成 (Code Structure)
- **`ProximityChat.java`**
  - プラグインのメインクラス。
  - `config.yml` の読み込みおよび設定値の保持。
  - Paperの `LifecycleEventManager` を使用した Brigadier コマンドの登録。
  - `GlobalRegionScheduler` を使用した非同期での設定保存。
- **`ChatListener.java`**
  - `AsyncChatEvent` をリッスンし、デフォルトのチャットをキャンセル。
  - 送信者と受信者のロケーションをそれぞれの `EntityScheduler` を介してスレッドセーフに取得し、距離を計算。
  - 距離に応じた Kyori Adventure `Component` を生成して送信。MiniMessage インジェクションを防ぐためのプレーンテキスト化も実施。
