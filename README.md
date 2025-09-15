# LandHealthRegenTimerTask

一个针对 Paper 1.21+ 的轻量级插件，按照固定间隔为玩家恢复生命值（默认每次 +1.0 HP，即半颗心），并可选保持饱和度，避免在恢复期间出现饥饿值消耗。支持与 Lands 插件联动，通过地皮角色旗帜控制是否允许定时回血，同时在开启 PvP 的区域自动禁用恢复。

## 特性
- 定时恢复：按固定周期为在线玩家回复生命（默认半颗心/次）。
- 饱和度保护：可选强制维持玩家饱和度为 1.0，减少饥饿消耗干扰。
- Lands 联动：
  - 新增角色旗帜 `health_regen_timer`（显示名：Health Regen）。
  - 仅在该旗帜允许，且区域 PvP 关闭时，才会对玩家进行恢复。
  - 若未安装 Lands，则默认不做地皮限制，直接按配置执行恢复。
- 事件友好：触发生命恢复事件 `EntityRegainHealthEvent`（原因：REGEN），便于其他插件拦截或联动。
- 性能友好：异步扫描玩家，仅在最终应用回血时切回主线程，减少对主线程的占用。

## 运行环境
- 服务端：Paper 1.21+（`api-version: 1.21`）
- Java 版本：Java 21
- 可选依赖：Lands（作为 softdepend 存在，不安装也可运行）

## 安装与使用
1. 将插件 `LandHealthRegenTimerTask-<version>.jar` 放入 `plugins/` 目录。
2. 首次启动后会生成默认配置 `config.yml`。
3. 如需与 Lands 联动：
   - 确保已安装并启用 Lands。
   - 在目标地皮的角色权限中启用 `Health Regen`（标识：`health_regen_timer`）。
   - 确保该区域 PvP 处于关闭状态，否则恢复会被自动禁用。

## 配置说明（config.yml）
```
regen-task:
  enabled: true                 # 是否启用定时恢复任务
  initial-delay-ticks: 200      # 首次延迟（tick），20 tick = 1 秒（默认 10 秒）
  period-ticks: 200             # 运行周期（tick），默认 200 tick = 10 秒
  prevent-saturation-loss: true # 是否维持饱和度为 1.0，避免恢复期间掉饱食度
```
说明：
- 每次恢复量为 1.0 HP（半颗心/次）；不会超过玩家最大生命。
- 修改配置后重启服务器或使用热重载（若你的环境支持）以生效。

## 与 Lands 的联动细节
- 插件在 onLoad 阶段注册角色旗帜 `health_regen_timer`，显示名为 `Health Regen`。
- 仅当：
  - 玩家所处位置的区域对其角色允许该旗帜；且
  - 该位置 PvP 关闭（Lands 角色旗帜 `attack_player` 为否）
  才会为该玩家执行定时恢复。
- 若无法从 Lands 获取 `attack_player` 旗帜，或发生查询异常，将“保守放行 PvP 检查”（视作 PvP 关闭），以避免过度阻断恢复功能，并在控制台输出警告日志。

## 工作原理与性能
- 异步任务定期扫描在线玩家并进行判定（Lands、血量、最大生命、饱和度策略等）。
- 真正的生命值变更在主线程执行，并先广播 `EntityRegainHealthEvent`，确保与其他插件协同一致。
- 该方案尽量减少主线程负担，适用于大多数常规生存服场景。

## 常见问题
- 没装 Lands 可以用吗？
  - 可以。没有 Lands 时，不做地皮与 PvP 限制，按配置直接定时恢复。
- 恢复量可以改吗？
  - 当前固定为 1.0 HP（半颗心/次），如需自定义请提出 issue 或自行修改源码。
- 为什么我在某些地皮内不回血？
  - 检查该地皮/角色是否允许 `Health Regen` 旗帜；
  - 检查该地皮 PvP 是否关闭；
  - 检查是否有其他插件通过事件取消了恢复。

## 构建
- 使用 Maven：`mvn clean package`
- 生成的可分发文件位于 `target/`，同时提供普通与 shaded 版本。

## 版本
- 1.0.0：初始发布，支持异步定时恢复与 Lands 角色旗帜控制。

欢迎反馈与建议！
