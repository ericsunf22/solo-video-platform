# 本地视频播放管理平台 - 技术方案书与执行计划书

## 一、深度需求分析与细化

### 1.1 核心功能模块深度分析

#### 1.1.1 视频来源管理

**本地文件夹导入**
- **功能描述**：用户可以选择本地文件夹，系统自动扫描其中的视频文件并添加到视频库
- **技术要点**：
  - 前端需要提供文件夹选择功能（使用 `webkitdirectory` 属性）
  - 后端需要实现文件系统扫描服务，递归遍历文件夹
  - 需要支持视频格式过滤（.mp4, .avi, .mkv, .mov, .flv 等）
  - 需要处理重复视频的检测（基于文件路径或哈希值）
- **用户体验**：
  - 扫描过程中显示进度条
  - 扫描完成后显示新增视频数量
  - 支持选择多个文件夹进行扫描

**手动上传视频**
- **功能描述**：用户可以手动选择视频文件上传到平台本地库
- **技术要点**：
  - 前端实现多文件上传功能
  - 后端实现文件接收和存储服务
  - 需要配置上传文件大小限制（建议单文件最大 2GB）
  - 需要实现文件上传进度显示
  - 需要处理文件名冲突（重命名或覆盖选项）
- **用户体验**：
  - 支持拖拽上传
  - 上传过程中显示每个文件的进度
  - 上传完成后可以立即查看视频信息

#### 1.1.2 视频播放核心

**视频播放功能**
- **功能描述**：原生流畅播放本地和上传的视频文件
- **技术要点**：
  - 使用 HTML5 Video 元素实现基础播放
  - 考虑使用 video.js 或 plyr 等播放器库增强功能
  - 需要处理不同浏览器对视频格式的支持差异
  - 实现视频流服务，支持大文件分片加载
- **用户体验**：
  - 视频加载过程中显示加载动画
  - 支持键盘快捷键控制（空格暂停/播放，左右箭头快进/快退等）

**播放控制功能**
- **功能描述**：提供完整的播放控制功能
- **技术要点**：
  - 播放/暂停控制
  - 进度条拖拽定位
  - 音量控制（滑块调节 + 静音按钮）
  - 倍速播放（0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x）
  - 全屏播放（支持浏览器全屏和网页全屏）
  - 快进/快退按钮（建议 10 秒步进）
- **用户体验**：
  - 控制栏在鼠标移出视频区域后自动隐藏
  - 显示当前播放时间和总时长

**播放记录功能**
- **功能描述**：自动保存播放记录，包括播放进度、时间和次数
- **技术要点**：
  - 定时保存播放进度（建议每 10 秒保存一次）
  - 记录最后播放时间
  - 累计播放次数
  - 记录视频总观看时长
- **用户体验**：
  - 下次播放时自动从上次停止的位置继续
  - 显示"继续播放"按钮
  - 播放历史列表中显示播放进度条

#### 1.1.3 视频资产管理

**收藏功能**
- **功能描述**：支持视频收藏和取消收藏，收藏列表独立展示
- **技术要点**：
  - 实现收藏状态的切换
  - 收藏列表的查询和展示
  - 支持批量收藏和取消收藏
- **用户体验**：
  - 视频卡片上显示收藏状态图标
  - 收藏列表中可以快速取消收藏

**标签系统**
- **功能描述**：支持为视频添加多个自定义标签，按标签筛选和检索
- **技术要点**：
  - 标签的创建、编辑、删除
  - 视频与标签的多对多关系
  - 标签云展示
  - 按标签筛选视频
  - 支持标签搜索
- **用户体验**：
  - 视频详情页中显示标签列表
  - 点击标签可以快速筛选相关视频
  - 支持批量添加标签
  - 支持标签自动补全

**视频信息展示**
- **功能描述**：展示视频的基本信息
- **技术要点**：
  - 视频封面：支持自动提取视频第一帧或手动上传
  - 文件名：显示原始文件名
  - 时长：精确到秒，格式化为 HH:MM:SS
  - 格式：显示视频文件格式（MP4, AVI 等）
  - 上传/导入时间：显示添加到库的时间
  - 文件大小：显示视频文件大小
  - 分辨率：显示视频分辨率（如 1920x1080）
- **用户体验**：
  - 视频卡片显示关键信息
  - 视频详情页显示完整信息
  - 支持编辑视频名称和描述

#### 1.1.4 基础交互功能

**视图切换**
- **功能描述**：支持视频列表视图和网格视图切换
- **技术要点**：
  - 列表视图：显示详细信息，适合快速浏览
  - 网格视图：显示视频封面和标题，适合视觉浏览
  - 视图状态持久化保存
- **用户体验**：
  - 一键切换视图
  - 保持当前筛选条件和排序

**搜索功能**
- **功能描述**：按视频名称和标签搜索视频
- **技术要点**：
  - 支持关键词搜索（视频名称）
  - 支持标签搜索
  - 支持组合搜索（名称 + 标签）
  - 搜索结果高亮显示
  - 搜索历史记录
- **用户体验**：
  - 实时搜索建议
  - 搜索结果排序选项（相关度、时间、名称）
  - 搜索历史快速访问

**数据持久化**
- **功能描述**：确保播放记录、收藏和标签数据的持久化存储
- **技术要点**：
  - 使用 H2DB 作为开发环境数据库
  - 使用 JPA 实现数据访问层
  - 数据库结构设计合理，支持高效查询
  - 数据备份和恢复机制（可选）
- **用户体验**：
  - 数据自动保存，用户无感知
  - 系统重启后数据不丢失

### 1.2 非功能性需求分析

#### 1.2.1 性能要求
- 视频加载时间：< 3 秒（本地视频）
- 页面响应时间：< 1 秒
- 支持并发播放用户数：单机部署支持 100+ 并发用户
- 视频库容量：支持 10,000+ 视频文件

#### 1.2.2 安全性要求
- 文件上传安全：限制文件类型和大小，防止恶意文件上传
- 路径遍历防护：防止通过文件路径访问系统敏感文件
- 输入验证：对所有用户输入进行验证，防止 SQL 注入和 XSS 攻击
- 访问控制：本地部署环境下，默认允许本地网络访问，可配置访问权限

#### 1.2.3 可用性要求
- 界面简洁直观，操作流程简单
- 提供必要的帮助信息和操作提示
- 支持响应式设计，适配不同屏幕尺寸
- 错误处理友好，提供明确的错误提示

#### 1.2.4 可维护性要求
- 代码结构清晰，模块化设计
- 遵循编码规范，注释完整
- 日志记录完善，便于问题排查
- 配置文件集中管理，便于部署和调整

---

## 二、技术方案书

### 2.1 系统架构设计

#### 2.1.1 整体架构
采用标准前后端分离架构，前端负责用户界面和交互，后端提供 RESTful API 服务。

```
┌─────────────────────────────────────────────────────────────┐
│                        前端应用层                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  视频管理    │  │  播放控制    │  │  资产管理（收藏/标签）  │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                    状态管理与服务层                        │  │
│  │  (React Context / Zustand + Axios HTTP Client)          │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        API 网关层                              │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              RESTful API 接口（JSON 格式）                  │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        后端服务层                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  视频服务    │  │  播放服务    │  │  资产服务（收藏/标签）  │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ 文件扫描服务  │  │ 文件存储服务  │  │      搜索服务         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        数据访问层                              │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              Spring Data JPA Repository                   │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        数据存储层                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  H2DB 数据库  │  │  文件系统    │  │    视频文件存储       │  │
│  │  (开发环境)   │  │  (元数据)    │  │    (实际视频文件)     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.2 技术选型

**前端技术栈**
| 技术 | 版本 | 用途 |
|------|------|------|
| Vite | 5.x | 构建工具，快速开发和打包 |
| React | 18.x | 前端框架 |
| ShadCN UI | 最新 | UI 组件库 |
| TailWindCSS | 4.x | 样式框架 |
| React Router | 6.x | 路由管理 |
| Axios | 1.x | HTTP 客户端 |
| Zustand | 4.x | 状态管理（轻量级） |
| Lucide React | 最新 | 图标库 |
| video.js / plyr | 最新 | 视频播放器 |

**后端技术栈**
| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | Java 开发环境 |
| Spring Boot | 3.2.x | 核心框架 |
| Spring Data JPA | 3.2.x | 数据持久化 |
| H2 Database | 2.2.x | 开发环境数据库 |
| MySQL | 8.x | 生产环境数据库 |
| Lombok | 1.18.x | 代码简化工具 |
| Apache Commons IO | 2.15.x | 文件操作工具 |
| FFmpeg | 6.x | 视频处理（可选，用于提取封面和元数据） |
| Spring Validation | 3.2.x | 参数验证 |
| Spring Web | 3.2.x | Web 服务 |

#### 2.1.3 项目结构设计

**前端项目结构**
```
solo-video-platform/
├── frontend/
│   ├── public/
│   │   └── favicon.ico
│   ├── src/
│   │   ├── components/
│   │   │   ├── ui/                    # ShadCN UI 基础组件
│   │   │   │   ├── button.tsx
│   │   │   │   ├── input.tsx
│   │   │   │   ├── card.tsx
│   │   │   │   ├── dialog.tsx
│   │   │   │   ├── dropdown-menu.tsx
│   │   │   │   ├── badge.tsx
│   │   │   │   ├── tabs.tsx
│   │   │   │   ├── slider.tsx
│   │   │   │   ├── progress.tsx
│   │   │   │   └── ...
│   │   │   ├── video/
│   │   │   │   ├── VideoPlayer.tsx    # 视频播放器组件
│   │   │   │   ├── VideoCard.tsx      # 视频卡片组件
│   │   │   │   ├── VideoList.tsx      # 视频列表组件
│   │   │   │   ├── VideoGrid.tsx      # 视频网格组件
│   │   │   │   └── VideoDetail.tsx    # 视频详情组件
│   │   │   ├── layout/
│   │   │   │   ├── Header.tsx         # 头部组件
│   │   │   │   ├── Sidebar.tsx        # 侧边栏组件
│   │   │   │   └── MainLayout.tsx     # 主布局组件
│   │   │   ├── common/
│   │   │   │   ├── SearchBar.tsx      # 搜索栏组件
│   │   │   │   ├── TagSelector.tsx    # 标签选择器
│   │   │   │   ├── UploadArea.tsx     # 上传区域
│   │   │   │   └── ViewToggle.tsx     # 视图切换组件
│   │   │   └── player/
│   │   │       ├── PlayerControls.tsx # 播放控制组件
│   │   │       ├── ProgressBar.tsx    # 进度条组件
│   │   │       └── VolumeControl.tsx  # 音量控制组件
│   │   ├── pages/
│   │   │   ├── Home.tsx               # 首页（视频库）
│   │   │   ├── Player.tsx             # 播放页面
│   │   │   ├── Favorites.tsx          # 收藏页面
│   │   │   ├── History.tsx            # 播放历史页面
│   │   │   ├── Tags.tsx               # 标签管理页面
│   │   │   └── Settings.tsx           # 设置页面
│   │   ├── hooks/
│   │   │   ├── useVideos.ts           # 视频相关 hooks
│   │   │   ├── usePlayer.ts           # 播放器相关 hooks
│   │   │   ├── useFavorites.ts        # 收藏相关 hooks
│   │   │   └── useTags.ts             # 标签相关 hooks
│   │   ├── services/
│   │   │   ├── api.ts                 # API 基础配置
│   │   │   ├── videoService.ts        # 视频服务
│   │   │   ├── playerService.ts       # 播放服务
│   │   │   ├── favoriteService.ts     # 收藏服务
│   │   │   └── tagService.ts          # 标签服务
│   │   ├── store/
│   │   │   ├── videoStore.ts          # 视频状态管理
│   │   │   ├── playerStore.ts         # 播放器状态管理
│   │   │   └── uiStore.ts             # UI 状态管理
│   │   ├── types/
│   │   │   ├── video.ts               # 视频类型定义
│   │   │   ├── player.ts              # 播放器类型定义
│   │   │   ├── tag.ts                 # 标签类型定义
│   │   │   └── api.ts                 # API 类型定义
│   │   ├── utils/
│   │   │   ├── format.ts              # 格式化工具
│   │   │   ├── time.ts                # 时间处理工具
│   │   │   └── file.ts                # 文件处理工具
│   │   ├── App.tsx                    # 根组件
│   │   ├── main.tsx                   # 入口文件
│   │   ├── index.css                  # 全局样式
│   │   └── router.tsx                 # 路由配置
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── tailwind.config.js
```

**后端项目结构**
```
solo-video-platform/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── solo/
│   │   │   │           └── video/
│   │   │   │               ├── VideoPlatformApplication.java    # 启动类
│   │   │   │               ├── config/
│   │   │   │               │   ├── WebConfig.java               # Web 配置
│   │   │   │               │   ├── FileStorageConfig.java       # 文件存储配置
│   │   │   │               │   └── OpenApiConfig.java           # API 文档配置
│   │   │   │               ├── controller/
│   │   │   │               │   ├── VideoController.java          # 视频控制器
│   │   │   │               │   ├── PlayerController.java         # 播放控制器
│   │   │   │               │   ├── FavoriteController.java       # 收藏控制器
│   │   │   │               │   ├── TagController.java            # 标签控制器
│   │   │   │               │   ├── UploadController.java         # 上传控制器
│   │   │   │               │   └── ScanController.java           # 扫描控制器
│   │   │   │               ├── service/
│   │   │   │               │   ├── VideoService.java             # 视频服务接口
│   │   │   │               │   ├── impl/
│   │   │   │               │   │   └── VideoServiceImpl.java     # 视频服务实现
│   │   │   │               │   ├── PlayerService.java            # 播放服务接口
│   │   │   │               │   ├── impl/
│   │   │   │               │   │   └── PlayerServiceImpl.java    # 播放服务实现
│   │   │   │               │   ├── FavoriteService.java          # 收藏服务接口
│   │   │   │               │   ├── impl/
│   │   │   │               │   │   └── FavoriteServiceImpl.java  # 收藏服务实现
│   │   │   │               │   ├── TagService.java               # 标签服务接口
│   │   │   │               │   ├── impl/
│   │   │   │               │   │   └── TagServiceImpl.java      # 标签服务实现
│   │   │   │               │   ├── FileStorageService.java       # 文件存储服务
│   │   │   │               │   ├── impl/
│   │   │   │               │   │   └── FileStorageServiceImpl.java
│   │   │   │               │   ├── FileScanService.java          # 文件扫描服务
│   │   │   │               │   ├── impl/
│   │   │   │               │   │   └── FileScanServiceImpl.java
│   │   │   │               │   └── VideoMetadataService.java    # 视频元数据服务
│   │   │   │               ├── repository/
│   │   │   │               │   ├── VideoRepository.java          # 视频仓库
│   │   │   │               │   ├── PlayHistoryRepository.java    # 播放历史仓库
│   │   │   │               │   ├── FavoriteRepository.java       # 收藏仓库
│   │   │   │               │   ├── TagRepository.java            # 标签仓库
│   │   │   │               │   └── VideoTagRepository.java       # 视频标签关联仓库
│   │   │   │               ├── entity/
│   │   │   │               │   ├── Video.java                    # 视频实体
│   │   │   │               │   ├── PlayHistory.java              # 播放历史实体
│   │   │   │               │   ├── Favorite.java                 # 收藏实体
│   │   │   │               │   ├── Tag.java                      # 标签实体
│   │   │   │               │   └── VideoTag.java                 # 视频标签关联实体
│   │   │   │               ├── dto/
│   │   │   │               │   ├── request/
│   │   │   │               │   │   ├── VideoUploadRequest.java
│   │   │   │               │   │   ├── FolderScanRequest.java
│   │   │   │               │   │   ├── PlayProgressRequest.java
│   │   │   │               │   │   ├── TagCreateRequest.java
│   │   │   │               │   │   └── VideoUpdateRequest.java
│   │   │   │               │   └── response/
│   │   │   │               │       ├── VideoResponse.java
│   │   │   │               │       ├── PlayHistoryResponse.java
│   │   │   │               │       ├── TagResponse.java
│   │   │   │               │       ├── ScanResultResponse.java
│   │   │   │               │       ├── UploadResultResponse.java
│   │   │   │               │       └── ApiResponse.java          # 统一响应
│   │   │   │               ├── exception/
│   │   │   │               │   ├── BusinessException.java        # 业务异常
│   │   │   │               │   ├── FileStorageException.java     # 文件存储异常
│   │   │   │               │   ├── VideoNotFoundException.java   # 视频未找到异常
│   │   │   │               │   └── GlobalExceptionHandler.java   # 全局异常处理
│   │   │   │               ├── mapper/
│   │   │   │               │   └── VideoMapper.java              # 对象映射
│   │   │   │               └── util/
│   │   │   │                   ├── FileUtil.java                 # 文件工具
│   │   │   │                   ├── VideoUtil.java                # 视频工具
│   │   │   │                   └── StringUtil.java               # 字符串工具
│   │   │   └── resources/
│   │   │       ├── application.yml                                # 应用配置
│   │   │       ├── application-dev.yml                            # 开发环境配置
│   │   │       ├── application-prod.yml                           # 生产环境配置
│   │   │       └── schema.sql                                     # 数据库初始化脚本
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── solo/
│   │                   └── video/
│   │                       ├── controller/
│   │                       │   └── VideoControllerTest.java
│   │                       ├── service/
│   │                       │   └── VideoServiceTest.java
│   │                       └── VideoPlatformApplicationTests.java
│   ├── pom.xml
│   └── mvnw
├── docs/
│   ├── API.md
│   └── DEPLOY.md
├── scripts/
│   ├── start.sh
│   ├── stop.sh
│   └── build.sh
└── README.md
```

### 2.2 数据库设计

#### 2.2.1 核心实体关系图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   Video     │       │  VideoTag   │       │    Tag      │
│─────────────│       │─────────────│       │─────────────│
│ id          │──────<│ video_id    │>──────│ id          │
│ title       │       │ tag_id      │       │ name        │
│ description │       └─────────────┘       │ color       │
│ file_path   │                               │ created_at  │
│ file_size   │                               └─────────────┘
│ duration    │
│ format      │
│ resolution  │
│ cover_path  │
│ is_favorite │       ┌─────────────┐
│ created_at  │       │PlayHistory  │
│ updated_at  │──────<│─────────────│
└─────────────┘       │ id          │
                      │ video_id    │
                      │ progress    │
                      │ play_time   │
                      │ play_count  │
                      │ last_played │
                      └─────────────┘
```

#### 2.2.2 表结构设计

**Video 表（视频表）**
```sql
CREATE TABLE video (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_path VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    duration BIGINT,
    format VARCHAR(20),
    resolution VARCHAR(20),
    cover_path VARCHAR(1000),
    source_type VARCHAR(20) NOT NULL, -- UPLOADED, SCANNED
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_file_path (file_path(255)),
    INDEX idx_title (title(100)),
    INDEX idx_created_at (created_at),
    INDEX idx_is_favorite (is_favorite)
);
```

**Tag 表（标签表）**
```sql
CREATE TABLE tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(20) DEFAULT '#3b82f6',
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);
```

**VideoTag 表（视频标签关联表）**
```sql
CREATE TABLE video_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_video_tag (video_id, tag_id),
    INDEX idx_tag_id (tag_id),
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);
```

**PlayHistory 表（播放历史表）**
```sql
CREATE TABLE play_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    video_id BIGINT NOT NULL,
    progress BIGINT DEFAULT 0,
    play_count INT DEFAULT 0,
    total_play_time BIGINT DEFAULT 0,
    last_played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_video_id (video_id),
    INDEX idx_last_played (last_played_at),
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE
);
```

### 2.3 API 接口设计

#### 2.3.1 统一响应格式

所有 API 响应遵循以下格式：

**成功响应**
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**错误响应**
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

**分页响应**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [ ... ],
    "total": 100,
    "page": 1,
    "size": 20,
    "totalPages": 5
  }
}
```

#### 2.3.2 视频管理接口

**1. 获取视频列表**
- **URL**: `GET /api/videos`
- **描述**: 分页获取视频列表，支持筛选和排序
- **参数**:
  - `page` (int, 可选): 页码，默认 1
  - `size` (int, 可选): 每页数量，默认 20
  - `keyword` (string, 可选): 搜索关键词
  - `tagIds` (array, 可选): 标签 ID 列表
  - `isFavorite` (boolean, 可选): 是否收藏
  - `sortBy` (string, 可选): 排序字段 (createdAt, title, duration, fileSize)
  - `sortOrder` (string, 可选): 排序方向 (asc, desc)，默认 desc
- **响应**: 分页视频列表

**2. 获取视频详情**
- **URL**: `GET /api/videos/{id}`
- **描述**: 根据 ID 获取视频详细信息
- **参数**:
  - `id` (long, 路径参数): 视频 ID
- **响应**: 视频详情对象

**3. 上传视频**
- **URL**: `POST /api/videos/upload`
- **描述**: 上传视频文件
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `file` (file, 必填): 视频文件
  - `title` (string, 可选): 视频标题，默认使用文件名
  - `description` (string, 可选): 视频描述
- **响应**: 上传结果，包含视频 ID

**4. 批量上传视频**
- **URL**: `POST /api/videos/upload/batch`
- **描述**: 批量上传多个视频文件
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `files` (array, 必填): 视频文件数组
- **响应**: 批量上传结果

**5. 更新视频信息**
- **URL**: `PUT /api/videos/{id}`
- **描述**: 更新视频的标题、描述等信息
- **参数**:
  - `id` (long, 路径参数): 视频 ID
  - `title` (string, 可选): 视频标题
  - `description` (string, 可选): 视频描述
- **响应**: 更新后的视频信息

**6. 删除视频**
- **URL**: `DELETE /api/videos/{id}`
- **描述**: 删除视频（同时删除相关的播放历史、收藏、标签关联）
- **参数**:
  - `id` (long, 路径参数): 视频 ID
- **响应**: 删除结果

**7. 批量删除视频**
- **URL**: `DELETE /api/videos/batch`
- **描述**: 批量删除多个视频
- **参数**:
  - `ids` (array, 必填): 视频 ID 数组
- **响应**: 删除结果

#### 2.3.3 文件夹扫描接口

**1. 扫描本地文件夹**
- **URL**: `POST /api/scan/folder`
- **描述**: 扫描指定本地文件夹中的视频文件
- **参数**:
  - `folderPath` (string, 必填): 文件夹路径
  - `recursive` (boolean, 可选): 是否递归扫描子文件夹，默认 true
  - `updateExisting` (boolean, 可选): 是否更新已存在的视频信息，默认 false
- **响应**: 扫描结果，包含新增视频数量、更新视频数量、跳过视频数量

**2. 获取扫描进度**
- **URL**: `GET /api/scan/progress`
- **描述**: 获取当前扫描任务的进度
- **响应**: 扫描进度信息

**3. 取消扫描**
- **URL**: `POST /api/scan/cancel`
- **描述**: 取消当前正在进行的扫描任务
- **响应**: 取消结果

#### 2.3.4 播放记录接口

**1. 保存播放进度**
- **URL**: `POST /api/player/progress`
- **描述**: 保存视频播放进度
- **参数**:
  - `videoId` (long, 必填): 视频 ID
  - `progress` (long, 必填): 播放进度（毫秒）
  - `duration` (long, 可选): 本次播放时长（毫秒）
- **响应**: 保存结果

**2. 获取播放进度**
- **URL**: `GET /api/player/progress/{videoId}`
- **描述**: 获取视频的最后播放进度
- **参数**:
  - `videoId` (long, 路径参数): 视频 ID
- **响应**: 播放进度信息

**3. 获取播放历史**
- **URL**: `GET /api/player/history`
- **描述**: 分页获取播放历史列表
- **参数**:
  - `page` (int, 可选): 页码，默认 1
  - `size` (int, 可选): 每页数量，默认 20
- **响应**: 分页播放历史列表

**4. 清空播放历史**
- **URL**: `DELETE /api/player/history`
- **描述**: 清空所有播放历史
- **响应**: 清空结果

#### 2.3.5 收藏接口

**1. 切换收藏状态**
- **URL**: `POST /api/favorites/toggle/{videoId}`
- **描述**: 切换视频的收藏状态（收藏/取消收藏）
- **参数**:
  - `videoId` (long, 路径参数): 视频 ID
- **响应**: 当前收藏状态

**2. 获取收藏列表**
- **URL**: `GET /api/favorites`
- **描述**: 分页获取收藏的视频列表
- **参数**:
  - `page` (int, 可选): 页码，默认 1
  - `size` (int, 可选): 每页数量，默认 20
  - `keyword` (string, 可选): 搜索关键词
  - `sortBy` (string, 可选): 排序字段
  - `sortOrder` (string, 可选): 排序方向
- **响应**: 分页收藏视频列表

**3. 批量收藏**
- **URL**: `POST /api/favorites/batch`
- **描述**: 批量收藏多个视频
- **参数**:
  - `videoIds` (array, 必填): 视频 ID 数组
- **响应**: 收藏结果

**4. 批量取消收藏**
- **URL**: `DELETE /api/favorites/batch`
- **描述**: 批量取消收藏多个视频
- **参数**:
  - `videoIds` (array, 必填): 视频 ID 数组
- **响应**: 取消收藏结果

#### 2.3.6 标签接口

**1. 获取所有标签**
- **URL**: `GET /api/tags`
- **描述**: 获取所有标签列表
- **参数**:
  - `keyword` (string, 可选): 搜索关键词
  - `sortBy` (string, 可选): 排序字段 (name, videoCount, createdAt)
  - `sortOrder` (string, 可选): 排序方向
- **响应**: 标签列表

**2. 创建标签**
- **URL**: `POST /api/tags`
- **描述**: 创建新标签
- **参数**:
  - `name` (string, 必填): 标签名称
  - `color` (string, 可选): 标签颜色，默认 '#3b82f6'
  - `description` (string, 可选): 标签描述
- **响应**: 创建的标签信息

**3. 更新标签**
- **URL**: `PUT /api/tags/{id}`
- **描述**: 更新标签信息
- **参数**:
  - `id` (long, 路径参数): 标签 ID
  - `name` (string, 可选): 标签名称
  - `color` (string, 可选): 标签颜色
  - `description` (string, 可选): 标签描述
- **响应**: 更新后的标签信息

**4. 删除标签**
- **URL**: `DELETE /api/tags/{id}`
- **描述**: 删除标签（同时删除所有视频与该标签的关联）
- **参数**:
  - `id` (long, 路径参数): 标签 ID
- **响应**: 删除结果

**5. 为视频添加标签**
- **URL**: `POST /api/tags/video/{videoId}`
- **描述**: 为视频添加一个或多个标签
- **参数**:
  - `videoId` (long, 路径参数): 视频 ID
  - `tagIds` (array, 必填): 标签 ID 数组
- **响应**: 添加结果

**6. 移除视频标签**
- **URL**: `DELETE /api/tags/video/{videoId}/{tagId}`
- **描述**: 移除视频的某个标签
- **参数**:
  - `videoId` (long, 路径参数): 视频 ID
  - `tagId` (long, 路径参数): 标签 ID
- **响应**: 移除结果

**7. 批量为视频添加标签**
- **URL**: `POST /api/tags/videos/batch`
- **描述**: 为多个视频批量添加相同的标签
- **参数**:
  - `videoIds` (array, 必填): 视频 ID 数组
  - `tagIds` (array, 必填): 标签 ID 数组
- **响应**: 添加结果

#### 2.3.7 视频流接口

**1. 视频流播放**
- **URL**: `GET /api/stream/{videoId}`
- **描述**: 提供视频流服务，支持断点续传和范围请求
- **参数**:
  - `videoId` (long, 路径参数): 视频 ID
- **Header**:
  - `Range` (string, 可选): 范围请求头，如 `bytes=0-1023`
- **响应**: 视频流数据

**2. 获取视频封面**
- **URL**: `GET /api/videos/{id}/cover`
- **描述**: 获取视频封面图片
- **参数**:
  - `id` (long, 路径参数): 视频 ID
- **响应**: 封面图片

**3. 生成视频封面**
- **URL**: `POST /api/videos/{id}/cover/generate`
- **描述**: 从视频中提取帧生成封面
- **参数**:
  - `id` (long, 路径参数): 视频 ID
  - `timePosition` (long, 可选): 提取帧的时间位置（毫秒），默认 1000
- **响应**: 生成的封面路径

### 2.4 核心技术实现方案

#### 2.4.1 前端核心实现

**1. 视频播放器组件**
- 使用 `video.js` 或 `plyr` 作为基础播放器
- 实现自定义播放控制界面
- 支持键盘快捷键
- 实现播放进度自动保存
- 支持倍速播放记忆

**2. 状态管理**
- 使用 Zustand 进行轻量级状态管理
- 管理视频列表、播放状态、UI 状态
- 实现状态持久化（使用 localStorage）

**3. 文件上传**
- 实现拖拽上传功能
- 实现多文件上传
- 显示上传进度
- 支持暂停/继续上传

**4. 路由管理**
- 使用 React Router 6 实现单页应用路由
- 实现懒加载和代码分割
- 配置路由守卫

**5. UI 组件**
- 使用 ShadCN UI 组件库
- 自定义主题配置
- 实现响应式设计

#### 2.4.2 后端核心实现

**1. 文件扫描服务**
- 使用 Java NIO 进行文件系统遍历
- 实现递归扫描算法
- 支持断点续扫
- 实现文件变化监控（可选）

**2. 视频元数据提取**
- 使用 FFmpeg 提取视频信息（时长、分辨率、格式等）
- 实现视频帧提取用于封面生成
- 支持常见视频格式的元数据解析

**3. 视频流服务**
- 实现 HTTP 范围请求支持
- 优化大文件内存占用
- 实现流式传输，避免内存溢出

**4. 文件存储服务**
- 实现文件上传接收
- 处理文件名冲突
- 实现文件访问权限控制
- 支持文件压缩（可选）

**5. 数据访问层**
- 使用 Spring Data JPA 实现 Repository
- 定义规范的查询方法
- 实现分页和排序
- 优化 N+1 查询问题

### 2.5 配置方案

#### 2.5.1 后端配置

**application.yml (基础配置)**
```yaml
server:
  port: 8080

spring:
  application:
    name: solo-video-platform
  
  servlet:
    multipart:
      max-file-size: 2GB
      max-request-size: 10GB
  
  datasource:
    url: jdbc:h2:file:./data/video_db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.H2Dialect

app:
  file:
    storage:
      path: ./storage/videos
      cover-path: ./storage/covers
      temp-path: ./storage/temp
    scan:
      supported-formats: mp4,avi,mkv,mov,flv,wmv,webm,m4v
  video:
    cover:
      extract-time: 1000
      quality: 0.8
    stream:
      buffer-size: 8192
```

**application-prod.yml (生产环境配置)**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/solo_video_platform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect

app:
  file:
    storage:
      path: /data/solo-video/videos
      cover-path: /data/solo-video/covers
      temp-path: /data/solo-video/temp
```

#### 2.5.2 前端配置

**vite.config.ts**
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
```

---

## 三、执行计划书

### 3.1 项目里程碑规划

| 阶段 | 任务 | 预计时间 | 交付物 |
|------|------|----------|--------|
| **阶段一** | 项目初始化与骨架搭建 | 2 天 | 前后端项目骨架、基础配置 |
| **阶段二** | 核心功能开发（视频管理） | 5 天 | 视频 CRUD、上传、扫描功能 |
| **阶段三** | 核心功能开发（播放系统） | 4 天 | 视频播放器、播放记录、进度保存 |
| **阶段四** | 核心功能开发（资产管理） | 4 天 | 收藏、标签系统、搜索功能 |
| **阶段五** | 前端界面完善与优化 | 3 天 | 完整的前端界面、响应式设计 |
| **阶段六** | 集成测试与 Bug 修复 | 3 天 | 测试报告、修复后的代码 |
| **阶段七** | 部署与文档 | 2 天 | 部署脚本、用户文档、API 文档 |
| **总计** | | **23 天** | |

### 3.2 详细开发计划

#### 阶段一：项目初始化与骨架搭建（第 1-2 天）

**第 1 天：后端项目初始化**
- [ ] 创建 Spring Boot 项目骨架
- [ ] 配置 Maven 依赖
- [ ] 配置 application.yml
- [ ] 创建基础目录结构
- [ ] 配置 H2 数据库
- [ ] 实现统一响应格式
- [ ] 实现全局异常处理
- [ ] 配置跨域支持
- [ ] 配置 API 文档（SpringDoc OpenAPI）

**第 2 天：前端项目初始化**
- [ ] 创建 Vite + React 项目
- [ ] 配置 TypeScript
- [ ] 安装并配置 TailwindCSS 4
- [ ] 初始化 ShadCN UI 组件库
- [ ] 配置项目目录结构
- [ ] 配置路由（React Router）
- [ ] 配置状态管理（Zustand）
- [ ] 配置 Axios HTTP 客户端
- [ ] 创建基础布局组件
- [ ] 配置环境变量

#### 阶段二：核心功能开发 - 视频管理（第 3-7 天）

**第 3 天：视频实体与数据层**
- [ ] 设计并创建 Video 实体
- [ ] 创建 VideoRepository
- [ ] 创建 Video DTO（Request/Response）
- [ ] 创建 VideoMapper 对象映射
- [ ] 实现基础的数据库操作测试

**第 4 天：视频上传功能**
- [ ] 实现后端文件上传服务（FileStorageService）
- [ ] 实现视频上传接口（UploadController）
- [ ] 处理文件名冲突
- [ ] 实现前端上传组件（拖拽上传）
- [ ] 实现上传进度显示
- [ ] 实现多文件上传

**第 5 天：文件夹扫描功能**
- [ ] 实现文件扫描服务（FileScanService）
- [ ] 实现递归扫描算法
- [ ] 实现视频格式过滤
- [ ] 实现重复视频检测
- [ ] 实现扫描进度跟踪
- [ ] 实现前端扫描界面

**第 6 天：视频元数据处理**
- [ ] 集成 FFmpeg 或实现视频元数据提取
- [ ] 提取视频时长、分辨率、格式等信息
- [ ] 实现视频封面提取
- [ ] 实现封面生成接口
- [ ] 实现视频信息更新功能

**第 7 天：视频 CRUD 接口与列表**
- [ ] 实现 VideoService
- [ ] 实现 VideoController 所有接口
- [ ] 实现分页查询
- [ ] 实现排序功能
- [ ] 实现前端视频列表页面
- [ ] 实现前端视频详情页面
- [ ] 实现视图切换（列表/网格）

#### 阶段三：核心功能开发 - 播放系统（第 8-11 天）

**第 8 天：视频流服务**
- [ ] 实现视频流控制器（StreamController）
- [ ] 支持 HTTP 范围请求
- [ ] 实现流式传输
- [ ] 优化大文件内存使用
- [ ] 测试不同浏览器兼容性

**第 9 天：视频播放器集成**
- [ ] 选择并集成视频播放器库（video.js 或 plyr）
- [ ] 自定义播放器控件
- [ ] 实现播放/暂停控制
- [ ] 实现进度条拖拽
- [ ] 实现音量控制
- [ ] 实现全屏播放

**第 10 天：播放控制与倍速**
- [ ] 实现快进/快退按钮
- [ ] 实现倍速播放功能
- [ ] 实现键盘快捷键
- [ ] 实现自动播放下一个（可选）
- [ ] 实现播放暂停时的状态保存

**第 11 天：播放记录系统**
- [ ] 设计并创建 PlayHistory 实体
- [ ] 实现 PlayHistoryRepository
- [ ] 实现播放进度保存接口
- [ ] 实现定时保存播放进度（前端）
- [ ] 实现播放历史列表接口
- [ ] 实现前端播放历史页面
- [ ] 实现从上次位置继续播放

#### 阶段四：核心功能开发 - 资产管理（第 12-15 天）

**第 12 天：收藏系统**
- [ ] 设计收藏功能（使用 Video 表的 is_favorite 字段或单独表）
- [ ] 实现 FavoriteService
- [ ] 实现收藏切换接口
- [ ] 实现收藏列表接口
- [ ] 实现批量收藏/取消收藏
- [ ] 实现前端收藏页面
- [ ] 实现视频卡片收藏状态显示

**第 13 天：标签系统（数据层与服务）**
- [ ] 设计并创建 Tag 实体
- [ ] 设计并创建 VideoTag 关联实体
- [ ] 创建相关 Repository
- [ ] 实现 TagService
- [ ] 实现标签的 CRUD 接口
- [ ] 实现视频标签关联接口

**第 14 天：标签系统（前端）**
- [ ] 实现前端标签管理页面
- [ ] 实现视频详情页的标签显示
- [ ] 实现标签选择器组件
- [ ] 实现点击标签筛选视频
- [ ] 实现批量添加标签
- [ ] 实现标签自动补全

**第 15 天：搜索功能**
- [ ] 实现后端搜索服务
- [ ] 支持按视频名称搜索
- [ ] 支持按标签搜索
- [ ] 支持组合搜索
- [ ] 实现搜索结果分页
- [ ] 实现前端搜索栏
- [ ] 实现搜索结果页面
- [ ] 实现搜索历史

#### 阶段五：前端界面完善与优化（第 16-18 天）

**第 16 天：UI/UX 优化**
- [ ] 完善整体布局和导航
- [ ] 实现响应式设计（适配移动端）
- [ ] 优化视频卡片样式
- [ ] 优化播放页面布局
- [ ] 添加加载动画和空状态
- [ ] 实现平滑过渡动画

**第 17 天：用户体验增强**
- [ ] 实现批量操作功能（删除、收藏、添加标签）
- [ ] 实现确认对话框
- [ ] 实现错误提示和成功提示
- [ ] 实现右键菜单（可选）
- [ ] 优化表单交互

**第 18 天：性能优化**
- [ ] 实现图片懒加载
- [ ] 实现视频列表虚拟滚动（大数据量）
- [ ] 优化 API 请求（防抖、节流）
- [ ] 实现状态持久化
- [ ] 优化打包体积

#### 阶段六：集成测试与 Bug 修复（第 19-21 天）

**第 19 天：后端测试**
- [ ] 编写 Controller 层单元测试
- [ ] 编写 Service 层单元测试
- [ ] 编写 Repository 层测试
- [ ] 测试文件上传功能
- [ ] 测试文件扫描功能
- [ ] 测试 API 接口边界情况

**第 20 天：前端测试与联调**
- [ ] 前后端接口联调
- [ ] 测试视频上传流程
- [ ] 测试视频播放流程
- [ ] 测试收藏和标签功能
- [ ] 测试搜索功能
- [ ] 跨浏览器测试（Chrome、Firefox、Edge）

**第 21 天：Bug 修复与优化**
- [ ] 修复测试中发现的 Bug
- [ ] 优化性能问题
- [ ] 完善错误处理
- [ ] 添加必要的日志
- [ ] 代码审查和重构

#### 阶段七：部署与文档（第 22-23 天）

**第 22 天：部署准备**
- [ ] 编写后端部署脚本
- [ ] 编写前端构建脚本
- [ ] 编写启动/停止脚本
- [ ] 配置生产环境参数
- [ ] 测试部署流程

**第 23 天：文档编写**
- [ ] 编写 API 文档
- [ ] 编写部署文档
- [ ] 编写用户使用指南
- [ ] 编写开发环境搭建指南
- [ ] 更新 README.md

### 3.3 技术风险与应对策略

| 风险项 | 风险等级 | 应对策略 |
|--------|----------|----------|
| 视频格式兼容性问题 | 中 | 1. 使用 FFmpeg 进行格式检测<br>2. 后端支持多种常见格式<br>3. 前端提示不支持的格式 |
| 大文件上传性能问题 | 高 | 1. 实现分片上传<br>2. 显示上传进度<br>3. 支持断点续传 |
| 视频流播放卡顿 | 中 | 1. 实现 HTTP 范围请求<br>2. 优化缓冲区大小<br>3. 考虑使用 CDN（生产环境） |
| 数据库性能瓶颈 | 中 | 1. 合理设计索引<br>2. 实现分页查询<br>3. 考虑使用缓存（可选） |
| 跨域问题 | 低 | 1. 后端配置 CORS<br>2. 开发环境使用代理<br>3. 生产环境使用 Nginx 反向代理 |
| FFmpeg 依赖问题 | 中 | 1. 提供 FFmpeg 安装指南<br>2. 实现不依赖 FFmpeg 的降级方案<br>3. 考虑使用纯 Java 的视频处理库 |

### 3.4 开发环境要求

#### 硬件要求
- CPU：4 核及以上
- 内存：8GB 及以上
- 硬盘：50GB 及以上可用空间

#### 软件要求
| 软件 | 版本要求 | 用途 |
|------|----------|------|
| JDK | 21 | Java 开发环境 |
| Node.js | 18+ | 前端开发环境 |
| npm/yarn | 最新 | 前端包管理 |
| Maven | 3.9+ | Java 项目构建 |
| FFmpeg | 6.0+ | 视频处理（可选但推荐） |
| IDE | VS Code / IntelliJ IDEA | 开发工具 |
| 浏览器 | Chrome 100+ / Firefox 100+ | 测试浏览器 |

#### 操作系统
- Windows 10/11
- macOS 12+
- Linux（Ubuntu 20.04+）

### 3.5 验收标准

#### 功能验收
- [ ] 视频上传功能正常，支持大文件上传
- [ ] 文件夹扫描功能正常，支持递归扫描
- [ ] 视频播放流畅，支持倍速、音量、全屏等控制
- [ ] 播放进度自动保存，下次播放从上次位置继续
- [ ] 收藏功能正常，收藏列表独立展示
- [ ] 标签系统完整，支持创建、编辑、删除、关联视频
- [ ] 搜索功能正常，支持按名称和标签搜索
- [ ] 列表/网格视图切换正常
- [ ] 批量操作功能正常

#### 性能验收
- [ ] 页面首屏加载时间 < 2 秒
- [ ] 视频列表加载时间 < 1 秒（1000 条数据）
- [ ] 视频开始播放时间 < 3 秒（本地文件）
- [ ] 搜索响应时间 < 500ms
- [ ] 页面操作无明显卡顿

#### 兼容性验收
- [ ] Chrome 浏览器功能完整
- [ ] Firefox 浏览器功能完整
- [ ] Edge 浏览器功能完整
- [ ] 1080p 分辨率显示正常
- [ ] 移动端响应式布局正常

#### 安全性验收
- [ ] 文件类型限制有效，无法上传非视频文件
- [ ] 路径遍历防护有效
- [ ] SQL 注入防护有效
- [ ] XSS 攻击防护有效

---

## 四、后续优化建议

### 4.1 功能扩展
1. **用户系统**：添加多用户支持、权限管理
2. **视频转码**：实时转码支持，适配不同设备
3. **字幕支持**：外挂字幕、字幕搜索
4. **播放列表**：创建和管理播放列表
5. **视频剪辑**：基础的视频剪辑功能
6. **云存储集成**：支持阿里云 OSS、AWS S3 等云存储

### 4.2 技术优化
1. **缓存系统**：引入 Redis 缓存热点数据
2. **消息队列**：使用 RabbitMQ 处理异步任务（扫描、转码）
3. **搜索引擎**：集成 Elasticsearch 增强搜索功能
4. **监控系统**：添加应用监控和日志分析
5. **容器化**：使用 Docker 容器化部署
6. **CI/CD**：配置自动化构建和部署流程

### 4.3 用户体验
1. **主题切换**：支持深色/浅色主题
2. **快捷键自定义**：允许用户自定义键盘快捷键
3. **播放统计**：详细的播放统计和数据分析
4. **智能推荐**：基于播放历史的视频推荐
5. **移动端 App**：开发移动端应用

---

**文档版本**：v1.0  
**创建日期**：2026-04-21  
**最后更新**：2026-04-21
