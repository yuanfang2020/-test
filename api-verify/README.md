# API 批量验证脚本（纯 Shell 版）

> 适用于短期验证：400 个接口 × 3 台服务器，统一成功标准 `rspcode=000000`。

## 1. 目录说明

```text
api-verify/
├── apis.csv                # 接口清单（name,path,body_file）
├── run_verify.sh           # 主执行脚本
├── payloads/               # 各接口请求体 JSON
│   ├── login.json
│   └── query_order.json
└── reports/                # 执行后自动生成报告
```

## 2. 依赖

- `bash`
- `curl`
- `grep`
- `sed`
- `awk`

> 不依赖 `jq`，适用于无额外安装权限的环境。

## 3. 接口清单格式

`apis.csv` 首行必须是表头：

```csv
name,path,body_file
login,/api/login,payloads/login.json
queryOrder,/api/order/query,payloads/query_order.json
```

## 4. 成功判定规则

每次调用满足以下条件判定为成功：

1. HTTP 状态码 = `200`
2. 响应体（JSON）包含：`"rspcode":"000000"`

脚本会自动压缩空白字符，兼容如下格式：

- `"rspcode":"000000"`
- `"rspcode" : "000000"`

## 5. 配置服务器

在 `run_verify.sh` 中修改 `SERVERS`：

```bash
SERVERS=(
  "srv1=http://10.0.0.11:8080"
  "srv2=http://10.0.0.12:8080"
  "srv3=http://10.0.0.13:8080"
)
```

## 6. 执行

```bash
cd api-verify
chmod +x run_verify.sh
./run_verify.sh
```

## 7. 报告说明

每台服务器会生成 3 份文件：

- `reports/srvX_summary.txt`：总数/成功/失败/通过率
- `reports/srvX_detail.log`：每个接口 PASS/FAIL 明细
- `reports/srvX_failed.csv`：失败接口清单（便于重跑）

## 8. 使用建议

1. 先用 10~30 个冒烟接口验证脚本与认证配置。
2. 再扩到 400 全量接口。
3. 建议避开业务高峰执行。
4. 对写操作接口，务必使用测试账号或可回滚数据。
