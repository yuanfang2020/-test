#!/usr/bin/env bash
set -u

# ========== 配置 ==========
CSV_FILE="apis.csv"
REPORT_DIR="reports"
TIMEOUT=15

# 三台服务器（请按环境修改）
SERVERS=(
  "srv1=http://10.0.0.11:8080"
  "srv2=http://10.0.0.12:8080"
  "srv3=http://10.0.0.13:8080"
)

mkdir -p "$REPORT_DIR"

# 检查依赖
command -v curl >/dev/null 2>&1 || { echo "curl not found"; exit 1; }
command -v grep >/dev/null 2>&1 || { echo "grep not found"; exit 1; }
command -v awk  >/dev/null 2>&1 || { echo "awk not found"; exit 1; }
[ -f "$CSV_FILE" ] || { echo "$CSV_FILE not found"; exit 1; }

echo "=== Start verify: $(date '+%F %T') ==="

for s in "${SERVERS[@]}"; do
  sname="${s%%=*}"
  base="${s#*=}"

  detail_file="${REPORT_DIR}/${sname}_detail.log"
  fail_file="${REPORT_DIR}/${sname}_failed.csv"
  summary_file="${REPORT_DIR}/${sname}_summary.txt"

  : > "$detail_file"
  echo "name,path,http_code,rspcode_match" > "$fail_file"

  echo "=== Running on ${sname} (${base}) ==="

  # 跳过 CSV 表头，逐行读取
  tail -n +2 "$CSV_FILE" | while IFS=',' read -r name path body_file; do
    # 清理 Windows 回车
    name="$(echo "$name" | tr -d '\r')"
    path="$(echo "$path" | tr -d '\r')"
    body_file="$(echo "$body_file" | tr -d '\r')"

    # 空行跳过
    [ -z "$name" ] && continue

    if [ ! -f "$body_file" ]; then
      echo "[FAIL] $name body_file not found: $body_file" | tee -a "$detail_file"
      echo "${name},${path},BODY_FILE_MISSING,false" >> "$fail_file"
      continue
    fi

    # 响应格式: body + \n + http_code
    resp="$(curl -sS -m "$TIMEOUT" \
      -H 'Content-Type: application/json' \
      -X POST "${base}${path}" \
      --data @"${body_file}" \
      -w $'\n%{http_code}' 2>/dev/null || true)"

    http_code="$(echo "$resp" | tail -n1)"
    body="$(echo "$resp" | sed '$d')"

    # 压缩空白以兼容 "rspcode" : "000000" 这类格式
    compact="$(echo "$body" | tr -d '\r\n\t ')"

    rsp_match="false"
    echo "$compact" | grep -q '"rspcode":"000000"' && rsp_match="true"

    if [ "$http_code" = "200" ] && [ "$rsp_match" = "true" ]; then
      echo "[PASS] $name http=$http_code rspcode=000000" >> "$detail_file"
    else
      echo "[FAIL] $name http=$http_code rspcode_match=$rsp_match" | tee -a "$detail_file"
      echo "${name},${path},${http_code},${rsp_match}" >> "$fail_file"
    fi
  done

  # 从明细汇总，避免 while 管道子 shell 变量作用域问题
  total_real=$(grep -E '^\[PASS\]|^\[FAIL\]' "$detail_file" | wc -l | awk '{print $1}')
  ok_real=$(grep -E '^\[PASS\]' "$detail_file" | wc -l | awk '{print $1}')
  fail_real=$(grep -E '^\[FAIL\]' "$detail_file" | wc -l | awk '{print $1}')

  if [ "$total_real" -gt 0 ]; then
    pass_rate=$(awk -v t="$total_real" -v o="$ok_real" 'BEGIN { printf "%.2f%%", (o*100)/t }')
  else
    pass_rate="0.00%"
  fi

  {
    echo "server=$sname"
    echo "base=$base"
    echo "total=$total_real"
    echo "ok=$ok_real"
    echo "fail=$fail_real"
    echo "pass_rate=$pass_rate"
  } > "$summary_file"

  echo "Summary [$sname] total=$total_real ok=$ok_real fail=$fail_real pass_rate=$pass_rate"
done

echo "=== Finished: $(date '+%F %T') ==="
echo "Reports in: $REPORT_DIR"
