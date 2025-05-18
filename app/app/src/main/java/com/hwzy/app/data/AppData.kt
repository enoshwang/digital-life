package com.hwzy.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

// 可搜索项的通用接口
interface SearchableItem {
    val id: String
    val title: String
    val icon: ImageVector
    val category: String
    val type: ItemType
    val tags: List<String> // 用于搜索的标签
}

// 项目类型枚举
enum class ItemType {
    DISCOVER,    // 发现页面的项目
    ARTICLE,     // 文章
}

// 发现页面的项目
data class DiscoverItem(
    override val id: String,
    override val title: String,
    override val icon: ImageVector,
    override val category: String,
    override val type: ItemType = ItemType.DISCOVER,
    override val tags: List<String> = emptyList()
) : SearchableItem

// 文章项目
data class ArticleItem(
    override val id: String,
    override val title: String,
    override val icon: ImageVector,
    override val category: String,
    val content: String,
    val author: String,
    val publishDate: String,
    override val type: ItemType = ItemType.ARTICLE,
    override val tags: List<String> = emptyList()
) : SearchableItem

object AppData {
    // 发现页面的项目
    private val discoverItems = listOf(
        // 生活服务类
        DiscoverItem("test", "测试", Icons.Filled.WbSunny, "生活服务", tags = listOf("测试", "生活")),
        DiscoverItem("camera", "相机", Icons.Filled.Camera, "生活服务", tags = listOf("相机", "拍照")),

        // 工具类
        DiscoverItem("sensor", "传感器", Icons.Filled.WbSunny, "工具箱", tags = listOf("传感器", "设备信息"))
    )

    // 示例文章
    private val articleItems = listOf(
        ArticleItem(
            id = "article1",
            title = "如何使用 Compose 构建现代化 UI",
            icon = Icons.AutoMirrored.Filled.Article,
            category = "技术",
            content = "这是一篇关于 Compose 的教程...",
            author = "张三",
            publishDate = "2024-03-20",
            tags = listOf("Compose", "Android", "UI", "教程")
        )
        // 可以添加更多文章
    )

    // 获取所有可搜索项
    private val allSearchableItems: List<SearchableItem> = discoverItems + articleItems

    // 按类型获取项目
    @Suppress("unused")
    fun getItemsByType(type: ItemType): List<SearchableItem> {
        return allSearchableItems.filter { it.type == type }
    }

    // 按类别获取项目
    @Suppress("unused")
    fun getItemsByCategory(category: String): List<SearchableItem> {
        return allSearchableItems.filter { it.category == category }
    }

    // 获取所有类别
    @Suppress("unused")
    val categories: List<String>
        get() = allSearchableItems.map { it.category }.distinct()

    // 获取所有发现页面的项目
    val discoverItemsList: List<DiscoverItem>
        get() = discoverItems

    // 搜索项目
    fun searchItems(query: String): List<SearchableItem> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            allSearchableItems.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.tags.any { it.contains(query, ignoreCase = true) }
            }
        }
    }

    // 按类型搜索项目
    @Suppress("unused")
    fun searchItemsByType(query: String, type: ItemType): List<SearchableItem> {
        return searchItems(query).filter { it.type == type }
    }
}
