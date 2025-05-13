# HTTPS 服务器

这是一个使用 Go 语言实现的 HTTPS 服务器，提供 API 接口用于获取 API key 信息。

## 功能特性

- 使用 HTTPS 安全协议
- 提供 RESTful API 接口
- 使用环境变量进行配置
- 支持 SSL/TLS 证书

## 安装和运行

1. 克隆项目
```bash
git clone https://github.com/yourusername/https_server_go.git
cd https_server_go
```

2. 安装依赖
```bash
go mod download
```

3. 配置环境变量
复制 `.env.example` 文件为 `.env`，并填写相应的配置信息：
```bash
cp .env.example .env
```

4. 准备 SSL 证书
将 SSL 证书文件放在 `certs` 目录下：
- `certs/server.crt`
- `certs/server.key`

5. 运行服务器
```bash
go run main.go
```

## API 接口

### 获取 API Key

- 路径：`/api/v1/hwzy/start_completion`
- 方法：GET
- 响应示例：
```json
{
    "api_key": "your_api_key",
    "status": "success"
}
```

## 环境变量说明

- `PORT`: 服务器端口号（默认：443）
- `SSL_CERT_FILE`: SSL 证书文件路径
- `SSL_KEY_FILE`: SSL 私钥文件路径
- `API_KEY`: API 密钥

## 安全说明

- 请确保 SSL 证书和私钥文件的安全存储
- 定期更新 API 密钥
- 建议在生产环境中使用更强的安全措施 