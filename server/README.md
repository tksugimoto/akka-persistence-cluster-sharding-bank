# 銀行口座入出金機能

## 起動方法
```
sbt run
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
