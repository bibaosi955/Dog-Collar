## 目标
- 修复 v1 分支登录/短信/JWT 的安全问题，满足 reviewer 要求，并保持实现最小。

## 范围
- JwtService: 禁止默认可预测 secret；非 test profile 缺失配置则 fail-fast。
- SmsService: 000000 仅 test 可用；默认 profile 必须拒绝；增加过期与限流/次数限制；提供 challenge 机制。
- AuthService: 禁止“手机号不存在自动创建并直接登录”的无限制模式；必须基于已发送验证码的 challenge。
- JwtAuthFilter: 无效 token 返回 401 或至少 debug 记录原因（这里按 401 处理）。
- SecurityConfig: 放行 /auth/**；额外 guard 防止误启 test profile。
- Tests: 至少 2 个，且 ./mvnw -q test 通过（JDK21）。

## 阶段
1) 项目勘察与现状确认 (complete)
2) 先写测试 (RED) -> 运行失败确认 (complete)
3) 最小实现 (GREEN) (complete)
4) 重构与清理 (REFACTOR) (complete)
5) 全量测试与回归 ./mvnw -q test (complete)
6) 仅新增 1 个 commit（不 amend） (complete)

## 约束/决定
- 内存实现仅用于 v1：使用 ConcurrentHashMap + 时间戳实现 TTL/限流/次数。
- challengeId 使用随机 UUID；verify/login 时必须携带 challengeId。
- 默认 profile 下短信未配置：send/verify 必须失败，避免“任何手机号=000000 即登录”。

## 风险点
- Spring profile 与测试环境识别：采用显式开关 `security.test-profile-allowed=true` 仅在 test 资源中配置。

## Errors Encountered
| Error | Attempt | Resolution |
|---|---:|---|
