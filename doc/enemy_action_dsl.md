# 怪物 Action DSL 与图片映射规范

> 适用范围：`core/src/main/resources/data/monsters/level*.json` 中的 `actionScript`、怪物被动字段和战斗立绘资源。

## 运行时入口

- `EnemyActionResolver.resolve(...)` 负责把当前 JSON 中的原版风格 `actionScript` 转成 `CombatAction`。
- `EnemyActionResolver.describeIntent(...)` 负责给战斗 UI 提供敌人意图文案。
- `CombatEngine` 调用 resolver；如果 resolver 返回 `null`，才会走旧基础 DSL fallback。
- `EnemyActionResolverTest` 和后端 CLI `selftest` 会扫描 `level1` 到 `level5` 的所有怪物 action，要求当前 JSON action 全部接入 resolver。

## 现有兼容格式

当前保留原版风格 action id，例如：

```text
atk_double_3
inc_strength_3
atk_debuff_blockred
def_block_10
atk_wither_strike
def_team_shield
heal_emergency
trigger_wither_puppet
```

新增怪物时优先沿用可读的语义化 id：

- 攻击：`atk_*`
- 防御：`def_*`
- 强化：`buff_*` 或 `inc_*`
- 诅咒/削弱：`curse_*`
- 回复：`heal_*`
- 特殊动作：使用动词短语，例如 `steal_block`、`transfer_curse`

如果新增 action id，必须同步：

1. 在 `EnemyActionResolver.resolve(...)` 中登记实际动作。
2. 在 `EnemyActionResolver.describeIntent(...)` 中补玩家可读意图。
3. 运行 `./gradlew test` 和后端 CLI `selftest`。

## 基础 fallback DSL

旧基础 DSL 仍保留兼容，但不建议新怪物优先使用：

```text
atkN / attackN      对玩家造成 N 点伤害
atkNxM / attackNxM  对玩家造成 N 点伤害，重复 M 次
defN / blockN       敌人获得 N 点格挡
idle / stun         无行动
```

复杂行为不要塞进字符串解析规则里，应使用语义化 action id 并在 resolver 中显式登记。

## 数据检查

新增或修改 `level*.json` 后至少运行：

```bash
./gradlew :core:test --tests com.fabricatedbook.core.engine.EnemyActionResolverTest
printf 'selftest\nquit\n' | ./gradlew runBackendDebug
```

通过标准：

- `EnemyActionResolverTest` 成功。
- CLI 输出 `已配置怪物 actionScript 均已接入 EnemyActionResolver`。
- CLI 最终输出 `SELFTEST PASS`。

如果失败，按输出中的 `level/group/enemy -> actionId` 定位并补 resolver。

## 图片映射规范

战斗立绘由 `EnemyActor` 加载：

```text
core/src/main/java/com/fabricatedbook/view/actor/EnemyActor.java
```

加载规则：

1. 优先查 `NAME_TO_FILE` 的中文怪物名到文件名映射。
2. 未命中时尝试使用 `enemy.getName().toLowerCase()` 作为文件名。
3. 图片路径固定为 `core/src/main/resources/img/<fileName>.png`。
4. 读取失败时会降级为无立绘渲染，但这不应作为正式内容状态。

新增怪物时应同步：

- 在 `level*.json` 中确认 `name` 是稳定显示名。
- 在 `EnemyActor.NAME_TO_FILE` 中补中文名到图片文件名映射。
- 将图片放入 `core/src/main/resources/img/`。
- 如果复用已有图片，也显式写映射，避免依赖小写中文名 fallback。

命名建议：

- 图片文件名使用 ASCII、下划线或 PascalCase，避免空格。
- JSON 中同名敌人可共享同一个映射。
- 多个变体如果视觉不同，应使用不同中文名或新增更明确的映射规则。
