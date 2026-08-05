# API ABI 基线

`ikaros-api-1.2.2.jar` 是从完成 upstream 融合并升级至 `1.2.2` 后的提交
`bce916310e601a387bebbf025265288a9187988b` 构建的 API ABI 基线。

文件的 SHA-256 为：

```text
692E5D7D2ADA2944D3C04E9F8B4799B01785BD8B78F5A09007917E2F15D4B5D4
```

执行以下命令检查当前 API 是否与基线二进制兼容：

```shell
./gradlew :api:checkApiAbi
```

缺少基线或存在二进制不兼容变更时，任务会直接失败。此时应恢复旧 API，或在明确接受破坏性变更后升级主版本。发布新版本后，应从对应发布标签构建并新增版本化基线文件，同时更新 `api/build.gradle` 中的基线版本；不得覆盖已有基线。
