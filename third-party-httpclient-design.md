# 第三方接口 HTTPClient 设计（SpringBoot2.x / Java8）

## 设计目标
- 支持 `GET`、`POST` 两种请求。
- 支持不同参数传参方式：
  - GET：query string 或整体密文 `cipherText`
  - POST：`application/json`、`application/x-www-form-urlencoded`
- 请求头包含认证信息、签名信息。
- 敏感字段（手机号、卡号、证件号等）做字段级加密。
- 请求报文支持整体加密，算法可配置。
- 异常、日志完整，便于生产排障。
- 代码保持简洁：配置、加密、签名、请求发送职责分离。

## 核心类
- `ThirdPartyClientProperties`：统一配置。
- `FieldEncryptionService`：递归处理 `Map/List`，命中敏感字段后加密。
- `PayloadEncryptionService`：请求报文整体加密。
- `SignatureService`：基于 `HmacSHA256` 生成签名。
- `ThirdPartyHttpClientService`：总入口，负责组装请求、写日志、异常处理。
- `ThirdPartyRequestException`：统一业务异常。

## 配置示例
```yaml
third-party:
  http:
    base-url: https://api.partner.com
    app-id: demo-app
    auth-token: Bearer xxxxx
    signature-secret: sign-secret
    field-key: field-encrypt-key
    payload-key: payload-encrypt-key
    field-encryption-algorithm: AES   # NONE/AES/DES
    payload-encryption-algorithm: AES # NONE/AES/DES
    enable-field-encryption: true
    enable-payload-encryption: true
    encrypt-get-query: false
    sensitive-fields: mobile,phone,cardNo,bankCard,idCard,certNo
```

## 调用示例
```java
ThirdPartyRequest request = new ThirdPartyRequest();
request.setMethod(HttpMethod.POST);
request.setEndpoint("/order/create");
request.setContentType(MediaType.APPLICATION_JSON);
request.setParams(new HashMap<String, Object>() {{
    put("name", "张三");
    put("mobile", "13800000000");
    put("idCard", "110101199001011234");
}});

String response = thirdPartyHttpClientService.execute(request);
```

## 日志策略
- 请求日志：traceId、method、url、headers、body（预览截断，防止日志过大）。
- 响应日志：traceId、status、耗时、响应体。
- 异常日志：traceId、耗时、错误信息、完整堆栈。

## 异常策略
- 参数校验失败：直接抛 `ThirdPartyRequestException`。
- 加密/签名/序列化失败：转为 `ThirdPartyRequestException`。
- 网络请求失败：统一转换为 `ThirdPartyRequestException`，并输出完整上下文日志。
