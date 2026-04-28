#!/usr/bin/env bash
set -euo pipefail

# Sealos 推荐入口脚本只负责启动，不包含构建步骤。
# 约定：发布前已在开发环境完成 `mvn clean package -DskipTests`。

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

# Sealos 可通过环境变量注入端口；默认 8080。
PORT="${PORT:-8080}"

# 默认使用 Maven 打包产物名；若不存在则回退匹配 target 下第一个 jar。
JAR_PATH="${JAR_PATH:-target/atomic-focus.jar}"
if [[ ! -f "$JAR_PATH" ]]; then
  CANDIDATE="$(ls target/*.jar 2>/dev/null | head -n 1 || true)"
  if [[ -n "$CANDIDATE" ]]; then
    JAR_PATH="$CANDIDATE"
  fi
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "ERROR: 未找到可运行的 jar 文件。"
  echo "请先在开发环境执行: mvn clean package -DskipTests"
  echo "当前期望路径: $JAR_PATH"
  exit 1
fi

echo "Starting app with JAR: $JAR_PATH"
echo "Listening on port: $PORT"

# 显式监听 0.0.0.0，确保容器网络可访问。
exec java -jar "$JAR_PATH" \
  --server.port="$PORT" \
  --server.address=0.0.0.0
