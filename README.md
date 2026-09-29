# YinwuFlightBlock 飞行方块

Folia 兼容的飞行方块插件（移植自 NeoForge mod [BBL-Flight-Blocks](https://github.com/benbenlaw/BBL-Flight-Blocks)）。

放置飞行方块后，方块周围 **range**（默认 32）格内的**非创造/非旁观**玩家自动获得飞行能力；走出范围自动收回。只回收本插件启用的飞行。

## 功能

- 范围内玩家自动开启/关闭飞行（每 `check-interval` tick 检查一次，换维度/重进服立即重检）
- 右键方块切换「显示范围」：DUST 粒子画出范围立方体轮廓（每 `outline-interval` tick 刷新给所有已启用飞行的玩家）
- 合成配方：铁栏杆 + 烟花火箭 + 铁锭（ABA/CCC）
- 方块位置持久化到 `plugins/YinwuFlightBlock/flightblocks.yml`，重启保留
- 周期验证方块是否仍存在，处理爆炸/活塞等非玩家破坏

## 命令（权限 `yinwu.flightblock.admin`，默认 op）

```
/flightblock give <玩家> [数量]    # 发放飞行方块
/flightblock list                  # 列出所有已放置的飞行方块
/flightblock remove [世界] <x> <y> <z>   # 移除指定位置的飞行方块
/flightblock reload                # 重载配置
```

别名：`/fb`

## 配置（config.yml）

| 项 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 是否启用插件（false 则加载时跳过初始化） |
| `block-material` | `LIGHT_BLUE_STAINED_GLASS` | 飞行方块使用的原版材质（Bukkit 无法注册自定义方块） |
| `range` | `32` | 生效范围（格，以方块为中心的立方体，每轴 ±range） |
| `check-interval` | `20` | 飞行检查间隔（tick，20 = 1 秒） |
| `outline-interval` | `20` | 范围轮廓刷新间隔（tick，≤20 保持连续可见） |
| `outline-step` | `1` | 轮廓描边步长（调大减少粒子量） |
| `outline-particle` | `DUST` | 轮廓粒子类型（DUST 醒目，可换 END_ROD 等） |
| `outline-particle-color` | `87CEFA` | 轮廓粒子颜色（RRGGBB 十六进制，仅 DUST 生效） |
| `outline-particle-size` | `1.5` | 轮廓粒子大小（仅 DUST 生效） |
| `recipe-enabled` | `true` | 是否注册合成配方 |
| `messages.*` | 中文 | 聊天消息 |

## 已知限制

- **飞行能力是单一布尔**：若另一插件也 `setAllowFlight(true)`，本插件回收时会一并关闭（参考 mod 有同样限制）。
- **插件 jar 被永久删除后重启**：当时带飞行标记的玩家会保留飞行（标记存在玩家 NBT 上无人回收）。插件在卸载/热重载时会尽力即时回收；万一被永久删除，重装一次插件触发自愈即可。

## 构建

```bash
mvn -pl YinwuFlightBlock -am clean package
```

产物：`YinwuFlightBlock/target/YinwuFlightBlock-1.0.0.jar`
