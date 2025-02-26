#XBAPI - Minecraft 网页端 API 插件 🎮🌐
欢迎使用 XBAPI！这是一个专为 Minecraft 服务器设计的插件，旨在为网页端提供 API 支持。无论你是想开发一个玩家管理面板、签到系统。XBAPI 都能满足你的需求！🚀

目录 📑
功能介绍

安装步骤

API 接口

编译过程

常见问题

贡献指南

联系我们

功能介绍 🎮
XBAPI 的主要功能是为网页端提供 API 支持，具体功能包括：

玩家金币管理：查询玩家金币、增减金币。

签到系统：玩家每日签到，领取奖励。

在线玩家列表：实时获取服务器在线玩家列表。

服务器状态监控：获取服务器性能数据（CPU、内存等）。

高性能：基于高效算法，确保 API 响应迅速。

易扩展：支持自定义 API 开发，满足你的个性化需求。

安装步骤 🛠️
1. 下载插件
将 XBAPI.jar 文件下载到你的 plugins 目录。

复制
将文件放入plugins

2. 重启服务器
重启或者reload你的 Minecraft 服务器，让插件生效。

3. 配置插件
在 plugins/XBAPI/config.yml 中修改配置，例如：

# API 监听端口
api-port: 8080

# 签到奖励
sign-reward: 100

4. 享受功能 🎉
现在，你的服务器已经支持 XBAPI 的所有功能了！快去试试吧！

API 接口 🌐
XBAPI 提供了以下 API 接口，供网页端调用：

1. 获取玩家金币
URL: /xbapi/balance?player=<玩家名>

方法: GET/POST

返回:
{
  "status": "success",
  "player": "xuebi_test",
  "balance": 2000.0
}

2. 玩家签到
URL: /xbapi/sign?player=<玩家名>

方法: GET/POST

返回:
{
  "status": "success",
  "reward": 100
}

3. 获取在线玩家列表
URL: /xbapi/players

方法: GET/POST

返回:
{
  "players": ["player1", "player2", "player3"]
}

编译过程 🔧
如果你想自己编译 XBAPI，可以按照以下步骤操作：

1. 克隆代码库
bash
复制
git clone https://github.com/kk123s/XBAPI.git
cd XBAPI
2. 安装依赖
确保你已经安装了 Maven，然后运行：

复制
mvn clean install
3. 编译插件
mvn package
编译完成后，你可以在 target/ 目录下找到 XBAPI.jar 文件。

常见问题 ❓
1. 插件无法加载？
确保你的服务器版本与插件兼容。

检查 config.yml 配置是否正确。

2. API 请求失败？
确保服务器已启动并正常运行。

检查 API 地址是否正确。

3. 如何自定义功能？
你可以通过修改源代码或开发自定义插件来扩展功能。

贡献指南 🤝
欢迎贡献代码！以下是贡献步骤：

Fork 本仓库。

创建新分支：git checkout -b feature/你的功能。

提交更改：git commit -m '添加了某某功能'。

推送到分支：git push origin feature/你的功能。

提交 Pull Request。

联系我们 📞
如果有任何问题或建议，欢迎联系我们：

邮箱: 1972845799@qq.com

QQ:1972845799

GitHub: 提交 Issue

结尾 🎊
感谢你使用 XBAPI！希望它能为你带来愉快的 Minecraft 网页端开发体验。如果有任何问题，记得随时联系我们哦！😄

