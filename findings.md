## 发现
- 现有 JwtService 使用 `@Value("${security.jwt.secret:dev-only-secret-...}")` 提供可预测默认 secret（需要移除并在非 test 下缺失即 fail-fast）。
- 现有 SmsService 为内存 stub，固定验证码 `000000` 对所有 profile 生效（存在“任意手机号=000000 即登录”的洞）。
- 现有 AuthService：验证码校验通过后，若手机号不存在则自动创建用户并直接签发 JWT（缺少“必须来自已发送验证码会话”的约束）。
- 现有 JwtAuthFilter 捕获 `JwtException` 后静默吞掉，继续匿名下游鉴权（需要返回 401 或至少 debug 记录原因）。
- 现有 SecurityConfig 放行 `/auth/**`，其余全要求 authenticated；当前工程无统一异常处理（AuthException 直接抛会变成 500）。

## 决策记录
- 采用 challengeId：`/auth/sms/send` 返回 challengeId；`/auth/login/sms` 必须携带 challengeId + phone + code；SmsService 内存保存 challenge 并 enforce only-if-sent。
- 为了让非 test profile 的“短信未配置”失败更可测且语义明确，引入 `@RestControllerAdvice` 将 UnsupportedOperationException 映射为 501。
