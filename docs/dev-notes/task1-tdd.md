# Task 1 - TDD 记录（初始化 Spring Boot 单体骨架）

## 可审计证据（RED -> GREEN）

| 步骤 | 命令 | 结果（摘要） | 备注 |
| --- | --- | --- | --- |
| Step 1 (RED 准备) | 创建 `src/test/java/com/acme/collar/AppSmokeTest.java` | 新增 `@SpringBootTest` 的 `contextLoads()` | 测试先行 |
| Step 2 (RED) | `./mvnw -q test` | 失败：`./mvnw: No such file or directory` | 当时仓库没有 Maven Wrapper |
| Step 3-4 (GREEN) | 添加 `pom.xml` / 生产代码 / Maven Wrapper | `./mvnw -q test` 通过 | 骨架最小化，不引入业务实现 |

说明：此文件用于保留 Task 1 按 TDD 执行过“先失败再修复”的最小证据，不包含大段日志。
