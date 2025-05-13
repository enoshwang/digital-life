package main

import (
	"log"
	"os"

	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
)

type APIKeyResponse struct {
	APIKey string `json:"api_key"`
	Status string `json:"status"`
}

func main() {
	// 加载环境变量
	if err := godotenv.Load(); err != nil {
		log.Printf("Warning: .env file not found")
	}

	// 设置 Gin 模式
	gin.SetMode(gin.ReleaseMode)

	// 创建 Gin 路由
	r := gin.Default()

	// 注册路由
	v1 := r.Group("/api/v1/hwzy")
	{
		v1.GET("/start_completion", handleStartCompletion)
	}

	// 获取证书路径
	certFile := os.Getenv("SSL_CERT_FILE")
	keyFile := os.Getenv("SSL_KEY_FILE")

	if certFile == "" || keyFile == "" {
		log.Fatal("SSL certificate and key files must be specified in environment variables")
	}

	// 启动 HTTPS 服务器
	port := os.Getenv("PORT")
	if port == "" {
		port = "443"
	}

	log.Printf("Starting HTTPS server on port %s...", port)
	if err := r.RunTLS(":"+port, certFile, keyFile); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

func handleStartCompletion(c *gin.Context) {
	// 从环境变量获取 API key
	apiKey := os.Getenv("API_KEY")
	if apiKey == "" {
		c.JSON(500, gin.H{
			"status":  "error",
			"message": "API key not configured",
		})
		return
	}

	response := APIKeyResponse{
		APIKey: apiKey,
		Status: "success",
	}

	c.JSON(200, response)
} 