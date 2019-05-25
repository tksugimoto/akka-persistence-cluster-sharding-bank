# 銀行口座入出金機能

## 起動方法
1. [Apache Cassandra](https://cassandra.apache.org/) を `127.0.0.1:9042` で起動
    - IP:PORTを変更する場合は、環境変数 `CASSANDRA_CONTACT_POINTS` を設定する
        - `127.0.0.1:19042`
        - `192.168.99.100`
            - ※ PORT が `9042` （デフォルト）の場合は省略可能
1. Server起動
    ```
    sbt run
    ```
    ```
    sbt -Dhttp.port=8081 -Dakka.remote.netty.tcp.port=0 run
    ```
    ```
    sbt -Dhttp.port=8082 -Dakka.remote.netty.tcp.port=0 run
    ```

## API

- 残高確認
    ```
    GET /account/123/balance
    ```
- 預け入れ
    ```
    POST /account/123/deposit?amount=100
    ```
- 引き出し
    ```
    POST /account/123/withdraw?amount=100
    ```
