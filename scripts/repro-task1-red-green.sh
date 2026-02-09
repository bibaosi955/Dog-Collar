#!/usr/bin/env bash
set -u

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
  say "=== 环境信息 ==="
  say "[信息] java -version："
  java -version 2>&1 || true
  say

  local major
  major="$(java_major_version)"

  if [ "${major}" != "21" ]; then
    say "=== Green 阶段（跳过）==="
    say "[提示] 当前 Java 主版本为：${major:-未知}，不是 21。"
    say "[提示] 本项目需要 Java 21 才能验证 green。"
    say "[提示] 请按 README 设置 JDK 21 后重试：README.md"
    say "[提示] 本次运行未验证 green（但 red 已完成）。"
    return 0
  fi

  say "=== Green 阶段（应通过）==="
  say "[步骤] 使用 Java 21 运行 ./mvnw -q test。"

  local out
  out="$(./mvnw -q test 2>&1)"
  local rc=$?

  if [ ${rc} -eq 0 ]; then
    say "[结果] 退出码：0（通过）。"
  else
    say "[结果] 退出码：${rc}（未通过）。"
  fi
  say "[结果] 输出摘要（截取末尾关键行）："
  printf '%s\n' "${out}" | tail -n 25

  return ${rc}
}

main() {
  need_clean_worktree
  run_expected_red
  run_green_if_java21
}

main "$@"
