# motherbase

マザーベースとは、組織管理やアカウント管理などのサービスの基盤機能を提供するプラットフォームです。

## 特徴

- DDD/Clean Architecture/CQRS+Event Sourcingをベースにしたリアクティブアーキテクチャ
- AWSで稼働する前提
- Kubernetes(EKS)対応

## システム構成

![](backend/docs/system-layout.png)

### アプリケーション構成

- Write API
- Domain Event Router
- Read Model Updater
- Read API

### ストレージ構成

- ジャーナルデータベース
    - DynamoDB
- スナップショットデータベース
    - S3
- イベントバス
    - Kafka
- リードモデルデータベース
    - Aurora(RDS)

## アクター

- システムオーナー
    - 当該システムを運営する主体
- サービスオーナー
    - システムオーナーの許諾を受けてサービスを提供する主体
- サービスカスタマー
    - サービスオーナーの許諾を受けてサービスを利用する主体

## 機能

### アカウント管理コンテキスト

- システムオーナー向け機能
    - システムオーナー・アカウント管理
    - サービス管理
    - サービスオーナー管理
    - サービスオーナー・アカウント管理
- サービスオーナー向け機能
    - 自サービスオーナー管理
    - サービスカスタマー管理
    - サービスカスタマー・アカウント管理
- サービスカスタマー向け機能
    - 自サービスカスタマー管理
    - 自サービスカスタマー・アカウント管理
    - ポリシー管理
    - ユーザアカウント管理
    - ユーザグループ管理
    - ロール管理
