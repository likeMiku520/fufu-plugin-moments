# fufu-plugin-moments

Halo 2.0 的瞬间管理插件，提供一个轻量级的内容发布功能，支持发布图文、视频、音频等内容。

> 本仓库是 [halo-sigs/plugin-moments](https://github.com/halo-sigs/plugin-moments) 的复刻版本，在官方插件的基础上增加了**自定义点赞数量**、**定时发布**、**图片顺序调整**三项功能，其余能力与上游保持一致。

![Preview](./images/plugin-moments-preview.png)

## 相比官方版本的额外功能

### 自定义点赞数量

- 发布瞬间时点右下角的点赞按钮（爱心图标）即可指定点赞数量，已发布的瞬间在编辑模式下也能修改。
- 数值写入 Halo 的计数器，而主题端展示读取的正是它，所以访客点赞会在你设定的数值上继续累加，不是写死的假数字。
- 编辑时如果没有改动这个数字，保存不会回写计数器，不会覆盖真实点赞。

### 定时发布

- 发布时间选择器支持选择未来的时间，选好后按钮上会显示「定时发布：2026-10-26 09:00」。
- 到点之前，前台列表、`/moments` 主题路由、RSS、公共 API、站内搜索都不会出现这条瞬间；到点自动出现。
- 是否可见按请求实时判断，不依赖任何定时任务，因此服务重启或停机都不会导致漏发。
- 后台瞬间列表会用「定时发布」标记待发布的瞬间；编辑时点「恢复当前时间」即可改为立即发布。

### 图片顺序调整

- 未发布时（发布框或编辑模式），每张图片左上角有眼睛按钮，点开后图片的上下左右出现箭头。
- 点击箭头会与对应方向的相邻图片交换位置，箭头会跟随图片移动，再点一次眼睛即可退出。
- 只有一张图片时不显示该按钮；已发布后的展示效果不受影响。

## 功能特性

- 支持发布图文、视频、音频等多媒体内容
- 提供标签管理功能，支持按标签筛选瞬间
- 主题端 `/moments` 列表路由和 `/moments/{name}` 详情路由
- 提供匿名可访问的公共 REST API，方便前端框架构建客户端渲染瞬间列表
- RSS 订阅支持，路由为 `/moments/rss.xml`
- 支持将瞬间数据同步至 Halo 搜索系统
- 支持通过 MCP Server 查询、获取、发布和删除瞬间

## 安装使用

1. 下载，在 [Releases](https://github.com/likeMiku520/fufu-plugin-moments/releases) 中下载最新版本的 JAR 文件。
2. 安装，插件安装和更新方式可参考：<https://docs.halo.run/user-guide/plugins>
3. 安装完成之后，访问 Console 左侧的**瞬间**菜单项，即可进行管理。
4. 前台访问地址为 `/moments`，需要注意的是，此插件需要主题提供模板（`moments.html`）才能访问 `/moments`。
5. 此插件也提供了 RSS 订阅的路由，可以访问 `/moments/rss.xml`。
6. 此插件将数据同步至 Halo 搜索，type 为 `moment.moment.halo.run`。

> 环境要求：Halo >= 2.26.0。

> 本插件与官方版本使用相同的插件名（`PluginMoments`），安装本版本会替换掉官方版本，两者无法同时安装。

## 版本说明

- 最新版本：[v1.2.1](https://github.com/likeMiku520/fufu-plugin-moments/releases/tag/v1.2.1) — 定时发布、图片顺序调整、自定义点赞数量，界面沿用官方原版配色。
- 历史版本与每个版本的更新内容见 [Releases](https://github.com/likeMiku520/fufu-plugin-moments/releases)。

## MCP Server 集成

安装并启用 [Halo MCP Server](https://github.com/halo-dev/plugin-mcp-server) 1.x 后，本插件会自动提供以下 MCP 工具：

| 工具 | 说明 |
| --- | --- |
| `PluginMoments__list_moments` | 分页查询瞬间，支持按作者、标签、可见性、审核状态和发布时间筛选 |
| `PluginMoments__get_moment` | 按资源名称获取瞬间详情、作者和统计信息 |
| `PluginMoments__create_moment` | 以当前 MCP 访问密钥所有者的身份发布图文瞬间 |
| `PluginMoments__delete_moment` | 按资源名称永久删除瞬间，调用前必须获得用户明确确认 |

需要在 MCP Server 的访问密钥设置中选择需要开放的工具。未安装 MCP Server 时，不影响瞬间插件的其他功能。

## 主题适配

此插件为主题端提供了：

- **列表路由** `/moments`（模板 `moments.html`）和**详情路由** `/moments/{name}`（模板 `moment.html`）
- **Finder API**（`momentFinder`）：可在主题任意位置渲染瞬间列表，无需依赖路由页面
- **公共 REST API**：供前端框架构建客户端渲染瞬间列表使用

详细的主题适配文档请参考：

- [主题 API 文档](./dev/theme-api.md) — 模板路由、模板变量、Finder API、类型定义
- [REST API 文档](./dev/rest-api.md) — 公共 API 端点说明

## 开发文档

- [开发环境搭建](./dev/development.md) — 本地开发、构建、测试

## 许可证

[GPL-3.0](./LICENSE)，与上游保持一致。