#!/usr/bin/env bash
set -u

OUTPUT_FILE_DEFAULT="docs/dev-notes/task1-repro-output.txt"

say() {
  printf '%s\n' "$*"
}

die() {
  say "[错误] $*"
  exit 1
}

need_clean_worktree() {
  if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    die "当前目录不是 Git 仓库，无法检测工作区是否干净。"
  fi

  # 注意：这里的“工作区不干净”按“工作树(working tree)”理解：
  # - 若存在未暂存的已跟踪文件变更：退出非 0
  # - 若存在未跟踪文件：退出非 0
  # - 允许已暂存变更（用于先 git add，再运行脚本，最后一次性 commit）
  # - 允许通过 REPRO_ALLOW_DIRTY=1 跳过检查（用于 CI 或一次性修复场景）

  if [ "${REPRO_ALLOW_DIRTY-}" = "1" ]; then
    say "[提示] REPRO_ALLOW_DIRTY=1：跳过 clean worktree 检查。"
    return 0
  fi

  if ! git diff --quiet; then
    say "[提示] 工作区不干净（存在未暂存的已跟踪文件变更）。"
    say "[提示] 为了保证可审计复现，请先提交或清理后再运行。"
    say
    say "当前状态（git status --porcelain）："
    git status --porcelain
    exit 2
  fi

  local untracked
  untracked="$(git ls-files --others --exclude-standard)"
  if [ -n "${untracked}" ]; then
    say "[提示] 工作区不干净（存在未跟踪文件）。"
    say "[提示] 为了保证可审计复现，请先提交或清理后再运行。"
    say
    say "未跟踪文件列表："
    printf '%s\n' "${untracked}"
    exit 2
  fi
}

java_major_version() {
  # 输出类似：21 / 17 / 1.8
  local first_line
  if ! first_line="$(java -version 2>&1 | head -n 1)"; then
    echo ""
    return 0
  fi

  # 典型：openjdk version "21.0.2" 2024-01-16
  # 或：java version "17.0.11" 2024-04-16 LTS
  local v
  v="$(printf '%s' "$first_line" | sed -n 's/.*version "\([0-9][0-9]*\)\..*".*/\1/p')"
  if [ -n "$v" ]; then
    printf '%s' "$v"
    return 0
  fi

  # 兼容 1.8 这种
  v="$(printf '%s' "$first_line" | sed -n 's/.*version "1\.\([0-9][0-9]*\)\..*".*/\1/p')"
  printf '%s' "$v"
}

java_major_version_of() {
  # 参数：java 可执行文件路径
  # 输出：主版本号（例如 21 / 17 / 8）；失败返回空字符串
  local java_cmd
  java_cmd="$1"
  if [ -z "${java_cmd}" ] || [ ! -x "${java_cmd}" ]; then
    echo ""
    return 0
  fi

  local first_line
  if ! first_line="$(${java_cmd} -version 2>&1 | head -n 1)"; then
    echo ""
    return 0
  fi

  local v
  v="$(printf '%s' "$first_line" | sed -n 's/.*version "\([0-9][0-9]*\)\..*".*/\1/p')"
  if [ -n "$v" ]; then
    printf '%s' "$v"
    return 0
  fi

  v="$(printf '%s' "$first_line" | sed -n 's/.*version "1\.\([0-9][0-9]*\)\..*".*/\1/p')"
  if [ -n "$v" ]; then
    printf '%s' "$v"
    return 0
  fi

  echo ""
}

find_java21_home() {
  # 返回：可用的 JDK 21 JAVA_HOME；否则返回空
  # 探测优先级（按需求）：
  # 1) JAVA_HOME（若已是 21）
  # 2) /usr/libexec/java_home -v 21（macOS）
  # 3) Homebrew 路径（Apple Silicon 常见）：/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  # 兼容：最后再尝试旧用法 JAVA21_HOME（若用户仍在使用）
  local cand

  cand="${JAVA_HOME-}"
  if [ -n "${cand}" ] && [ -x "${cand}/bin/java" ]; then
    if [ "$(java_major_version_of "${cand}/bin/java")" = "21" ]; then
      printf '%s' "${cand}"
      return 0
    fi
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    cand="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [ -n "${cand}" ] && [ -x "${cand}/bin/java" ]; then
      if [ "$(java_major_version_of "${cand}/bin/java")" = "21" ]; then
        printf '%s' "${cand}"
        return 0
      fi
    fi
  fi

  cand="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  if [ -x "${cand}/bin/java" ]; then
    if [ "$(java_major_version_of "${cand}/bin/java")" = "21" ]; then
      printf '%s' "${cand}"
      return 0
    fi
  fi

  cand="${JAVA21_HOME-}"
  if [ -n "${cand}" ] && [ -x "${cand}/bin/java" ]; then
    if [ "$(java_major_version_of "${cand}/bin/java")" = "21" ]; then
      printf '%s' "${cand}"
      return 0
    fi
  fi

  echo ""
}

_REPRO_MVNW_PATH=""
_REPRO_BAK_PATH=""
_REPRO_RENAMED="0"

restore_mvnw() {
  if [ "${_REPRO_RENAMED}" != "1" ]; then
    return 0
  fi
  if [ -f "${_REPRO_BAK_PATH}" ] && [ ! -f "${_REPRO_MVNW_PATH}" ]; then
    mv "${_REPRO_BAK_PATH}" "${_REPRO_MVNW_PATH}"
  fi
  _REPRO_RENAMED="0"
}

run_expected_red() {
  local mvnw_path="./mvnw"
  local bak_path="./mvnw.__bak"

  if [ ! -f "${mvnw_path}" ]; then
    if [ -f "${bak_path}" ]; then
      say "[提示] 检测到 ${mvnw_path} 不存在，但 ${bak_path} 存在；将先尝试恢复 mvnw。"
      mv "${bak_path}" "${mvnw_path}"
    else
      die "未找到 ${mvnw_path}，无法进行 red 复现。"
    fi
  fi
  if [ -e "${bak_path}" ]; then
    die "检测到 ${bak_path} 已存在，请先手动处理后再运行，以免覆盖。"
  fi

  say "=== Red 阶段（预期失败）==="
  say "[步骤] 临时将 mvnw 重命名为 mvnw.__bak，然后尝试运行 ./mvnw -q test。"

  _REPRO_MVNW_PATH="${mvnw_path}"
  _REPRO_BAK_PATH="${bak_path}"
  _REPRO_RENAMED="0"

  mv "${mvnw_path}" "${bak_path}"
  _REPRO_RENAMED="1"

  # 无论 red 执行中发生什么，都尽力还原 mvnw
  trap restore_mvnw EXIT INT TERM

  local out
  local rc
  out="$("${mvnw_path}" -q test 2>&1)"
  rc=$?

  say "[结果] 退出码：${rc}（预期为非 0）。"
  say "[结果] 失败摘要（截取关键行）："
  printf '%s\n' "${out}" | tail -n 25

  restore_mvnw
  trap - EXIT INT TERM

  say "[完成] red 阶段失败为预期行为，脚本将继续执行。"
  say
}

run_green_if_java21() {
  _REPRO_GREEN_RAN="0"

  say "=== 环境信息 ==="
  say "[信息] java -version："
  java -version 2>&1 || true
  say

  local major
  major="$(java_major_version)"

  local java21_home
  java21_home=""

  if [ "${major}" != "21" ]; then
    java21_home="$(find_java21_home)"
  fi

  if [ "${major}" != "21" ] && [ -z "${java21_home}" ]; then
    say "=== Green 阶段（跳过）==="
    say "[提示] 当前 Java 主版本为：${major:-未知}，不是 21，且未找到可用的 JDK 21。"
    say "[提示] 本项目需要 Java 21 才能验证 green。"
    say "[提示] 未能在本机验证 green（脚本将以退出码 0 结束）。"
    say "[提示] 可尝试：安装 JDK 21，并让 /usr/libexec/java_home -v 21 可用，或安装 Homebrew openjdk@21。"
    say "[提示] 或临时指定：JAVA_HOME=<JDK21_HOME> PATH=\"$JAVA_HOME/bin:$PATH\" 后重试。"
    return 0
  fi

  say "=== Green 阶段（应通过）==="
  if [ "${major}" = "21" ]; then
    say "[步骤] 使用当前 Java 21 运行 ./mvnw -q test。"
  else
    say "[步骤] 使用检测到的 JDK 21 运行 ./mvnw -q test。"
    say "[信息] JAVA_HOME=${java21_home}"
    say "[信息] ${java21_home}/bin/java -version："
    "${java21_home}/bin/java" -version 2>&1 || true
  fi

  local out
  local rc

  _REPRO_GREEN_RAN="1"

  if [ "${major}" = "21" ]; then
    out="$(./mvnw -q test 2>&1)"
    rc=$?
  else
    out="$(JAVA_HOME="${java21_home}" PATH="${java21_home}/bin:${PATH}" ./mvnw -q test 2>&1)"
    rc=$?
  fi

  if [ ${rc} -eq 0 ]; then
    say "[结果] 退出码：0（通过）。"
    say "[PASS] ./mvnw -q test 在 Java 21 下通过。"
  else
    say "[结果] 退出码：${rc}（未通过）。"
  fi
  say "[结果] 输出摘要（截取末尾关键行）："
  if [ -n "${out}" ]; then
    printf '%s\n' "${out}" | tail -n 25
  else
    say "<无输出（-q 模式常见）；以退出码为准>"
  fi

  return ${rc}
}

main() {
  local output_file
  output_file="${REPRO_OUTPUT_FILE-${OUTPUT_FILE_DEFAULT}}"
  mkdir -p "$(dirname "${output_file}")"

  need_clean_worktree

  # 重要：先完成 clean worktree 检查，再写入文件，避免误报。
  : >"${output_file}"
  # 说明：日志里可能出现开发用的随机密码等信息；这里做最小脱敏，保证可提交。
  exec > >(sed -E 's/^(Using generated security password:).*/\1 <REDACTED>/' | tee -a "${output_file}") 2>&1

  say "说明：以下为在本机实际执行 \`bash scripts/repro-task1-red-green.sh\` 的输出摘要（去除敏感信息）。"
  say

  run_expected_red
  run_green_if_java21
  local green_rc
  green_rc=$?
  if [ "${_REPRO_GREEN_RAN-0}" = "1" ]; then
    exit ${green_rc}
  fi

  exit 0
}

main "$@"
