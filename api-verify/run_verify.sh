#!/usr/bin/env bash
set -u

# ========== 配置 ==========
readonly CSV_FILE="apis.csv"
readonly REPORT_DIR="reports"
readonly TIMEOUT=15

# 三台服务器（请按环境修改）
readonly SERVERS=(
  "srv1=http://10.0.0.11:8080"
  "srv2=http://10.0.0.12:8080"
  "srv3=http://10.0.0.13:8080"
)

require_cmd() {
  # 作用：检查命令是否存在，不存在则退出
  # 参数：
  #   $1 - 命令名（如 curl/grep/awk）
  command -v "$1" >/dev/null 2>&1 || { echo "$1 not found"; exit 1; }
}

calc_pass_rate() {
  # 作用：根据 total 和 ok 计算通过率（保留两位小数）
  # 参数：
  #   $1 - total，总用例数
  #   $2 - ok，通过数
  # 输出：
  #   通过率字符串，如 66.67%
  local total="$1" ok="$2"
  [ "$total" -eq 0 ] && { echo "0.00%"; return; }
  awk -v t="$total" -v o="$ok" 'BEGIN { printf "%.2f%%", (o*100)/t }'
}

parse_response() {
  # 作用：解析 curl 拼接响应（body + 换行 + http_code）
  # 参数：
  #   $1 - 原始响应字符串
  # 输出：
  #   http_code,rsp_match（rsp_match 为 true/false）
  local resp="$1"
  local http_code body compact rsp_match="false"
  http_code="$(printf '%s\n' "$resp" | tail -n1)"
  body="$(printf '%s\n' "$resp" | sed '$d')"
  compact="$(printf '%s' "$body" | tr -d '\r\n\t ')"
  grep -q '"rspcode":"000000"' <<<"$compact" && rsp_match="true"
  printf '%s,%s\n' "$http_code" "$rsp_match"
}

run_case() {
  # 作用：执行单条 API 用例并写入结果文件
  # 参数：
  #   $1 - base，请求基础地址
  #   $2 - detail_file，明细日志文件
  #   $3 - fail_file，失败结果 CSV 文件
  #   $4 - name，用例名
  #   $5 - path，请求路径
  #   $6 - body_file，请求体 JSON 文件路径
  local base="$1" detail_file="$2" fail_file="$3"
  local name="$4" path="$5" body_file="$6"
  local resp parsed http_code rsp_match

  [ -z "$name" ] && return

  if [ ! -f "$body_file" ]; then
    echo "[FAIL] $name body_file not found: $body_file" | tee -a "$detail_file"
    echo "${name},${path},BODY_FILE_MISSING,false" >> "$fail_file"
    return
  fi

  resp="$(curl -sS -m "$TIMEOUT" \
    -H 'Content-Type: application/json' \
    -X POST "${base}${path}" \
    --data @"${body_file}" \
    -w $'\n%{http_code}' 2>/dev/null || true)"

  parsed="$(parse_response "$resp")"
  http_code="${parsed%%,*}"
  rsp_match="${parsed##*,}"

  if [ "$http_code" = "200" ] && [ "$rsp_match" = "true" ]; then
    echo "[PASS] $name http=$http_code rspcode=000000" >> "$detail_file"
  else
    echo "[FAIL] $name http=$http_code rspcode_match=$rsp_match" | tee -a "$detail_file"
    echo "${name},${path},${http_code},${rsp_match}" >> "$fail_file"
  fi
}

main() {
  # 作用：主流程入口（依赖检查 -> 遍历服务器 -> 读取 CSV -> 汇总报告）
  # 参数：无（使用脚本顶部配置）
  local s sname base detail_file fail_file summary_file
  local total ok fail pass_rate
  local name path body_file

  mkdir -p "$REPORT_DIR"
  require_cmd curl
  require_cmd grep
  require_cmd awk
  [ -f "$CSV_FILE" ] || { echo "$CSV_FILE not found"; exit 1; }

  echo "=== Start verify: $(date '+%F %T') ==="

  for s in "${SERVERS[@]}"; do
    sname="${s%%=*}"
    base="${s#*=}"
    detail_file="${REPORT_DIR}/${sname}_detail.log"
    fail_file="${REPORT_DIR}/${sname}_failed.csv"
    summary_file="${REPORT_DIR}/${sname}_summary.txt"

    : >"$detail_file"
    echo "name,path,http_code,rspcode_match" >"$fail_file"
    echo "=== Running on ${sname} (${base}) ==="

    while IFS=',' read -r name path body_file; do
      name="${name//$'\r'/}"
      path="${path//$'\r'/}"
      body_file="${body_file//$'\r'/}"
      run_case "$base" "$detail_file" "$fail_file" "$name" "$path" "$body_file"
    done < <(tail -n +2 "$CSV_FILE")

    total=$(grep -Ec '^\[PASS\]|^\[FAIL\]' "$detail_file")
    ok=$(grep -Ec '^\[PASS\]' "$detail_file")
    fail=$(grep -Ec '^\[FAIL\]' "$detail_file")
    pass_rate="$(calc_pass_rate "$total" "$ok")"

    {
      echo "server=$sname"
      echo "base=$base"
      echo "total=$total"
      echo "ok=$ok"
      echo "fail=$fail"
      echo "pass_rate=$pass_rate"
    } >"$summary_file"

    echo "Summary [$sname] total=$total ok=$ok fail=$fail pass_rate=$pass_rate"
  done

  echo "=== Finished: $(date '+%F %T') ==="
  echo "Reports in: $REPORT_DIR"
}

# 作用：脚本入口，透传命令行参数（当前未使用）
main "$@"
