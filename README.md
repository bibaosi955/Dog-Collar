# Dog-Collar

## 开发环境

本项目使用 Spring Boot 3.x + Java 21（class file version 65）。如果你用 Java 17 运行 `./mvnw test`，会看到类似“class file version 65 / 不支持发行版本 21”的报错。

### 安装/启用 JDK 21（macOS 示例）

使用 Homebrew：

```bash
brew install openjdk@21
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

然后在项目根目录运行：

```bash
./mvnw -q test
```
