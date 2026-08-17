# 数独在线排行榜服务器

零依赖的 Node.js 排行榜服务：接收玩家成绩、按模式返回前十名、提供个人统计。

## 本地运行

```bash
node leaderboard-server.mjs
```

默认监听 `http://0.0.0.0:8787`，成绩保存在同目录 `data/rankings.json`。
可用环境变量：`PORT`（端口）、`DATA_DIR`（数据目录）。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/score` | 提交成绩，JSON：`{mode, name, elapsed, mistakes}` |
| GET | `/api/ranking?mode=easy` | 该模式最快前十名 |
| GET | `/api/me?mode=easy&name=小明` | 个人最佳/最近/平均/局数 |
| GET | `/health` | 健康检查 |

`mode` 取值：`easy` / `normal` / `expert` / `super` / `daily` / `x_easy` / `x_normal` / `x_expert` / `hyper_easy` / `hyper_normal` / `hyper_expert`。

## 部署到公网（三选一）

### 方式一：Render（推荐，免费额度）

1. 打开 [render.com](https://render.com) 注册登录；
2. New → **Web Service** → 上传或关联本文件夹（`server/`）；
3. Build Command 留空，Start Command 填：
   ```
   node leaderboard-server.mjs
   ```
4. 部署完成后得到域名，形如 `https://sudoku-leaderboard.onrender.com`。

### 方式二：Railway

1. [railway.app](https://railway.app) 新建项目 → 上传 `server/` 文件夹；
2. 自动识别 `package.json`，Start 命令填 `node leaderboard-server.mjs`；
3. 部署后复制生成的域名。

### 方式三：自己的服务器 / VPS

上传 `server/` 后执行 `node leaderboard-server.mjs`（建议用 `pm2` 守护），再用 Nginx 反代到 8787 端口并绑定域名。

## 网页版接入

1. 打开数独网页版 → 「我」→「排行榜」；
2. 在"排行榜服务器地址"里填部署好的地址（如 `https://sudoku-leaderboard.onrender.com`），点保存；
3. 之后每局通关成绩会自动上传，排行榜显示"🌐 在线排行榜"；
4. 不填地址 = 仅本地排名，功能不受影响。

> 说明：服务端已放开 CORS，任何网页都能调用；数据默认存 JSON 文件，量大了可以自行换成 SQLite/PostgreSQL。
