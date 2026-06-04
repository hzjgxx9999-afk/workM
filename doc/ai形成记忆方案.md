下面给你一套**面向上线、多用户场景**的完整需求分析与实施方案。目标不是做“演示级记忆”，而是做一套**真实可用、可扩展、可审计、不串用户**的 AI 记忆系统。

------

# 一、项目目标定义

当前问题是：

- AI 在**当前会话**内能记住上下文
- 一旦用户切换对话，新建会话，AI 就“忘了”
- 后续上线后会有很多用户同时使用，必须做到：
  - **每个用户的记忆独立**
  - **跨会话可延续**
  - **不会串用户**
  - **支持长期偏好、事实信息、历史摘要**
  - **后续可扩展到向量检索、语义记忆**

所以本项目的目标应定义为：

> 构建一套面向多用户 AI 助手的“长期记忆管理系统”，使系统能够在保证用户隔离与数据安全的前提下，针对不同用户跨会话保存、检索、注入和更新长期记忆，从而提升问答连续性、个性化程度与业务服务能力。

------

# 二、需求分析

## 2.1 业务背景

你这个 AI 助手不是单人本地玩具，而是将来要接入 App 让大量用户使用，因此不能依赖“当前窗口上下文”充当记忆。必须新增一套**业务层记忆模块**，由后端统一管理。

用户每次发起新对话时，系统需要自动识别当前用户身份，检索该用户历史积累的稳定信息、偏好信息和相关历史摘要，并在调用大模型前作为上下文注入，使模型输出具备“延续感”和“个性化”。

------

## 2.2 核心需求

### 1）支持多用户独立记忆

不同用户的记忆必须严格隔离。
 张三的记忆不能影响李四，管理员也不能通过普通接口读取他人私有记忆。

### 2）支持跨会话记忆延续

用户在 A 会话中明确表达过的信息，在 B 会话中仍能被系统识别并利用。

### 3）支持当前会话上下文记忆

当前对话中的多轮问答仍需维持连续性，这是短期记忆能力。

### 4）支持长期记忆抽取

系统应能从对话中自动筛选值得长期保留的信息，而不是机械保存所有聊天内容。

### 5）支持记忆检索与注入

用户提问时，系统应优先检索与本次问题最相关的记忆，并拼装为模型输入。

### 6）支持记忆更新、覆盖、失效

用户偏好、身份信息、项目归属等可能变化，因此系统不能只追加，还要支持更新、覆盖与过期。

### 7）支持会话摘要压缩

长会话消息不能无限堆叠，否则成本高、性能差。需要摘要机制。

### 8）支持权限控制与审计

要能追踪某条记忆是从哪次会话、哪条消息抽取出来的，并支持后台审查和删除。

------

## 2.3 非功能需求

### 1）安全性

- 记忆必须按 `user_id` 隔离
- 会话必须校验归属关系
- 敏感记忆要限制存储或加密

### 2）性能

- 单次请求检索记忆应控制在毫秒级到几十毫秒级
- 首版可基于 MySQL 索引实现，无需上来就引入复杂向量库

### 3）可扩展性

- 后续可平滑升级到 Redis 缓存、向量检索、语义召回
- 支持增加 memory_type、importance、TTL 等机制

### 4）可维护性

- 记忆结构清晰
- 提取逻辑可配置
- Prompt 拼装逻辑可独立演进

------

# 三、系统边界与范围

## 3.1 本期实施范围

本期建议先实现以下内容：

- 用户会话管理
- 当前会话消息存储
- 用户长期记忆存储
- 会话摘要存储
- 调用前记忆检索
- 调用后记忆提取
- 用户隔离与归属校验
- 用户手动清理记忆能力
- 基础后台管理能力

## 3.2 暂不纳入首期的高级功能

这些可以二期做：

- 向量数据库语义召回
- 用户记忆图谱
- 自动冲突消解
- 个性化记忆评分模型
- 多模态记忆（图片/音频/位置）
- 群体画像/组织级共享记忆

------

# 四、整体实施思路

整体上采用“**短期记忆 + 长期记忆 + 摘要记忆**”三层结构。

## 4.1 短期记忆

当前会话中的近 N 轮消息，用于保持当前对话的连贯性。

## 4.2 长期记忆

按用户维度沉淀的稳定信息与偏好信息，用于跨会话延续。

## 4.3 摘要记忆

针对长会话做压缩总结，用于降低 token 成本并保留上下文主线。

------

# 五、系统架构方案

建议后端架构分为 6 个核心模块：

## 5.1 会话管理模块

负责新建会话、切换会话、查询会话列表、校验会话归属。

## 5.2 消息管理模块

负责保存用户消息、保存 AI 回复、查询最近上下文。

## 5.3 记忆管理模块

负责长期记忆的存储、检索、更新、删除、失效。

## 5.4 记忆提取模块

负责在每轮对话后，从消息中提取值得保留的长期信息。

## 5.5 Prompt 组装模块

负责将系统提示词、用户长期记忆、相关摘要、当前消息组合成最终请求。

## 5.6 大模型调用模块

负责与 Qwen/百炼等模型接口对接，支持普通调用和流式调用。

------

# 六、核心业务流程

## 6.1 新会话问答流程

### 步骤 1：用户进入 AI 页面

系统根据登录态识别当前用户 `user_id`。

### 步骤 2：创建或切换会话

如果是新会话，生成 `conversation_id`；如果是历史会话，则校验归属。

### 步骤 3：用户发送消息

前端提交：

- userId
- conversationId
- content

### 步骤 4：后端保存用户消息

将当前消息落库。

### 步骤 5：检索上下文

后端同时查询：

- 当前会话最近 N 条消息
- 当前用户长期记忆
- 该会话历史摘要
- 与本轮问题相关的重点记忆

### 步骤 6：组装 Prompt

按规则拼接：

- 系统角色设定
- 当前用户画像
- 相关长期记忆
- 当前会话摘要
- 最近消息
- 本轮用户输入

### 步骤 7：调用大模型

向 Qwen/百炼发送请求，获取回复。

### 步骤 8：保存 AI 回复

将 assistant 的回答落库。

### 步骤 9：记忆提取

对本轮对话进行解析，抽取新记忆或更新旧记忆。

### 步骤 10：必要时生成摘要

若会话长度超过阈值，则生成或更新会话摘要。

------

# 七、数据库设计方案

下面给你一套首版靠谱表结构。

------

## 7.1 会话表 `ai_conversation`

```
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    title VARCHAR(255) DEFAULT NULL COMMENT '会话标题',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CLOSED/DELETED',
    last_message_time DATETIME DEFAULT NULL COMMENT '最后消息时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status_time (user_id, status, last_message_time)
) COMMENT='AI会话表';
```

作用：

- 一个用户可有多个会话
- 所有会话都必须归属于某个用户
- 后续会话列表按 `user_id` 查

------

## 7.2 消息表 `ai_message`

```
CREATE TABLE ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：system/user/assistant',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '内容类型',
    tokens INT DEFAULT NULL COMMENT '估算token数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv_time (conversation_id, create_time),
    INDEX idx_user_time (user_id, create_time)
) COMMENT='AI消息表';
```

作用：

- 保存完整对话记录
- 当前会话上下文按 `conversation_id` 查询
- 用户历史行为可按 `user_id` 检索

------

## 7.3 用户长期记忆表 `ai_user_memory`

```
CREATE TABLE ai_user_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记忆ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    memory_key VARCHAR(100) NOT NULL COMMENT '记忆键',
    memory_value TEXT NOT NULL COMMENT '记忆值',
    memory_type VARCHAR(30) NOT NULL COMMENT '类型：PROFILE/PREFERENCE/FACT/SUMMARY',
    source_conversation_id BIGINT DEFAULT NULL COMMENT '来源会话ID',
    source_message_id BIGINT DEFAULT NULL COMMENT '来源消息ID',
    importance INT NOT NULL DEFAULT 1 COMMENT '重要度1-10',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DELETED/EXPIRED',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    last_hit_time DATETIME DEFAULT NULL COMMENT '最近命中时间',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_memory_key (user_id, memory_key),
    INDEX idx_user_memory_type (user_id, memory_type, status),
    INDEX idx_user_importance (user_id, importance, status)
) COMMENT='用户长期记忆表';
```

作用：

- 存储跨会话长期信息
- 核心隔离字段是 `user_id`
- `memory_key` 用于覆盖更新
- `importance` 用于排序
- `expire_time` 用于临时偏好或短期事实失效

------

## 7.4 会话摘要表 `ai_conversation_summary`

```
CREATE TABLE ai_conversation_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '摘要ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    summary_text TEXT NOT NULL COMMENT '摘要内容',
    round_no INT NOT NULL DEFAULT 1 COMMENT '摘要轮次',
    source_message_start_id BIGINT DEFAULT NULL COMMENT '摘要起始消息',
    source_message_end_id BIGINT DEFAULT NULL COMMENT '摘要结束消息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv_round (conversation_id, round_no),
    INDEX idx_user_conv (user_id, conversation_id)
) COMMENT='会话摘要表';
```

作用：

- 长会话压缩
- 避免每次都带全量消息给模型

------

## 7.5 记忆操作日志表 `ai_memory_log`

```
CREATE TABLE ai_memory_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    memory_id BIGINT DEFAULT NULL COMMENT '记忆ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/HIT/EXPIRE',
    old_value TEXT DEFAULT NULL COMMENT '旧值',
    new_value TEXT DEFAULT NULL COMMENT '新值',
    operator_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/USER/ADMIN',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_op_time (user_id, operation_type, create_time)
) COMMENT='记忆操作日志表';
```

作用：

- 支持可追溯

- 后台可审计

- 后续问题排查更容易

  # 八、记忆分类设计

  长期记忆不能乱存，建议分 4 类。

  ## 8.1 PROFILE：用户画像

  稳定身份信息，如：

  - 用户姓名
  - 职业
  - 工种
  - 所属项目
  - 常用语言

  特点：稳定、长期有效、权重高。

  ------

  ## 8.2 PREFERENCE：偏好信息

  如：

  - 喜欢简洁回答
  - 希望多给代码
  - 偏好中文
  - 喜欢语音回复

  特点：可更新、需覆盖旧值。

  ------

  ## 8.3 FACT：业务事实

  如：

  - 用户最近在开发工人端 AI 助手
  - 用户近期关注实时语音链路
  - 用户上次在问薪资计算问题

  特点：部分短期有效，可设置过期时间。

  ------

  ## 8.4 SUMMARY：高层总结

  如：

  - 用户近阶段主要在做 Android 端实时语音和 AI 接入
  - 最近几次会话集中在 AI 记忆与多用户隔离

  特点：适合模型快速建立背景。

  ------

  # 九、记忆提取策略

  这是系统成败关键之一。

  ## 9.1 不能做“全量保存”

  如果每条消息都记，后面一定变成垃圾库，检索质量会越来越差。

  ## 9.2 推荐“规则 + LLM”双通道

  ### 第一层：规则提取

  适合识别强信号句式：

  - “我是……”
  - “以后都……”
  - “我更喜欢……”
  - “记住……”
  - “我现在在做……”
  - “我的项目是……”

  优点：

  - 快
  - 便宜
  - 可控

  ### 第二层：LLM 提取

  对规则漏掉但很重要的信息，再交给小模型或主模型提取。

  输出格式建议固定为 JSON：

  ```
  [
    {
      "memory_key": "preferred_language",
      "memory_value": "中文",
      "memory_type": "PREFERENCE",
      "importance": 8,
      "action": "UPSERT"
    }
  ]
  ```

  ------

  ## 9.3 提取判定标准

  适合写入长期记忆的信息：

  - 稳定身份
  - 长期偏好
  - 多次提及的目标
  - 中长期项目背景
  - 后续仍可能用到的事实

  不适合长期记忆的信息：

  - 一次性寒暄
  - 无意义闲聊
  - 短时情绪
  - 明显临时信息
  - 敏感隐私但无业务必要的信息

  ------

  # 十、记忆检索策略

  ## 10.1 V1：基于规则和权重检索

  先不引入向量库，够用了。

  调用模型前查询：

  1. PROFILE 记忆
  2. PREFERENCE 记忆
  3. importance 高的 FACT
  4. 最近命中的 SUMMARY
  5. 当前会话摘要

  ------

  ## 10.2 检索排序建议

  按这个优先级：

  1. 用户画像
  2. 用户偏好
  3. 与当前问题关键词匹配的事实记忆
  4. 最近的会话摘要
  5. 当前会话最近 N 条消息

  ------

  ## 10.3 V2：语义检索扩展

  二期可以增加 embedding：

  - 对 memory_value 向量化
  - 对用户问题向量化
  - 做 TopK 召回

  这样用户问“我上次做到哪里了”，系统能召回最相关的记忆，而不是只靠关键词。

  ------

  # 十一、Prompt 组装方案

  这是最终体现“有记忆”的地方。

  建议结构如下：

  ```
  [System Instruction]
  你是工人端AI助手，回答准确、清晰、简洁，结合用户长期记忆回答。
  
  [User Profile]
  - 用户职业：全栈开发工程师
  - 常用语言：中文
  
  [User Preferences]
  - 偏好：回答直接一些
  - 偏好：优先给实施方案
  
  [Relevant Long-term Memory]
  - 用户正在开发工人端AI助手
  - 用户近期关注多用户跨会话记忆能力
  
  [Conversation Summary]
  - 该会话之前主要讨论AI记忆和多用户隔离
  
  [Recent Messages]
  user: …
  assistant: …
  user: …
  
  [Current User Query]
  ……
  ```

  注意：

  - 不要把几十条记忆全拼进去
  - 只拼核心、相关、去重后的内容
  - Prompt 模块要独立封装，便于后续优化

  ------

  # 十二、用户隔离与安全实施方案

  这部分必须严。

  ## 12.1 会话归属校验

  任意消息请求必须先校验：

  - `conversation_id` 是否属于当前 `user_id`

  否则直接拒绝。

  ------

  ## 12.2 记忆查询必须带 user_id

  任何长期记忆读取，都只能按当前用户查询。

  不能出现：

  ```
  SELECT * FROM ai_user_memory LIMIT 20
  ```

  必须是：

  ```
  SELECT * FROM ai_user_memory
  WHERE user_id = #{userId} AND status = 'ACTIVE'
  ```

  ------

  ## 12.3 敏感信息控制

  以下信息不建议默认作为长期记忆存储：

  - 身份证号
  - 银行卡号
  - 密码
  - 精确住址
  - 完整健康病史

  若业务必须使用：

  - 单独加密存储
  - 不参与普通 Prompt 注入
  - 管理员可见范围受控

  ------

  ## 12.4 支持用户删除

  建议对用户开放：

  - 查看我的 AI 记忆
  - 删除某条记忆
  - 清空所有长期记忆

  这对合规和用户信任很重要。

  ------

  # 十三、后端模块设计建议

  按 Spring Boot + MyBatis 结构，建议这样拆：

  ## 13.1 controller

  - `AiConversationController`
  - `AiChatController`
  - `AiMemoryController`

  ## 13.2 service

  - `AiConversationService`
  - `AiMessageService`
  - `AiMemoryService`
  - `AiMemoryExtractorService`
  - `AiSummaryService`
  - `AiPromptBuilderService`
  - `AiChatService`

  ## 13.3 mapper

  - `AiConversationMapper`
  - `AiMessageMapper`
  - `AiUserMemoryMapper`
  - `AiConversationSummaryMapper`
  - `AiMemoryLogMapper`

  ------

  # 十四、核心接口设计

  ## 14.1 创建会话

  ```
  POST /app/ai/conversation/create
  ```

  返回：

  - conversationId
  - title

  ------

  ## 14.2 发送消息

  ```
  POST /app/ai/chat/text
  ```

  请求体：

  ```
  {
    "conversationId": 1001,
    "content": "我上次问到哪了？"
  }
  ```

  处理流程：

  - 取当前登录用户
  - 校验 conversation 归属
  - 保存消息
  - 检索记忆
  - 组装 prompt
  - 调模型
  - 保存回复
  - 触发记忆提取

  ------

  ## 14.3 查询会话列表

  ```
  GET /app/ai/conversation/list
  ```

  按当前 `user_id` 查。

  ------

  ## 14.4 查询消息列表

  ```
  GET /app/ai/conversation/{id}/messages
  ```

  需校验会话归属。

  ------

  ## 14.5 查询我的记忆

  ```
  GET /app/ai/memory/list
  ```

  按当前用户查长期记忆。

  ------

  ## 14.6 删除某条记忆

  ```
  DELETE /app/ai/memory/{id}
  ```

  需校验该 memory 属于当前用户。

  ------

  ## 14.7 清空我的记忆

  ```
  POST /app/ai/memory/clear
  ```

  逻辑删除该用户所有 ACTIVE 记忆。

# 十五、缓存与性能优化建议

## 15.1 首版可以只用 MySQL

只要索引建好，单用户查 10~20 条记忆压力很小。

## 15.2 二期可加 Redis

适合缓存：

- 用户画像
- 用户偏好
- 最近摘要

比如 Key：

- `ai:memory:user:{userId}:profile`
- `ai:memory:user:{userId}:preferences`

------

## 15.3 控制上下文长度

推荐：

- 最近消息 10~20 条
- 长期记忆 5~15 条
- 摘要 1~3 条

不要无限拼装。

------

# 十六、异常场景处理

## 16.1 会话不存在

返回“会话不存在或无权限访问”。

## 16.2 记忆提取失败

不影响主流程，只记录日志。

## 16.3 摘要生成失败

不影响本次回复，下次重试。

## 16.4 记忆冲突

例如用户先说“以后简短回答”，后来又说“详细一点”。

处理规则：

- 同 `memory_key` 走覆盖更新
- 保留版本日志

------

# 十七、实施阶段建议

------

## 第一阶段：基础可用版

目标：上线能用，不串用户。

实现内容：

- 会话表、消息表、记忆表、摘要表
- 基础问答流程
- 按用户隔离的长期记忆
- 规则式记忆提取
- Prompt 注入
- 会话归属校验

交付结果：

- 用户跨会话能延续核心偏好与背景
- 多用户间不串数据

------

## 第二阶段：质量增强版

目标：记忆更准、成本更稳。

实现内容：

- LLM 提取长期记忆
- 会话摘要机制
- 记忆命中时间更新
- 记忆查看/删除功能
- 管理台审计页面

交付结果：

- 记忆质量明显提升
- 上下文成本下降
- 具备运维和审计能力

------

## 第三阶段：高级智能版

目标：更像“真有长期记忆”。

实现内容：

- embedding + 向量召回
- 语义相关记忆检索
- 记忆有效期与衰减
- 多类型记忆评分
- 个性化召回策略

交付结果：

- 面对复杂场景仍能稳定召回高相关信息
- AI 的“记住你”体验更自然

------

# 十八、风险点与避坑建议

## 风险 1：把所有聊天都当长期记忆

后果：记忆污染、召回失真。
 建议：严格筛选，只保留高价值内容。

## 风险 2：长期记忆只按 conversation_id 存

后果：切会话即丢失。
 建议：长期记忆必须按 `user_id` 存储。

## 风险 3：不做会话归属校验

后果：越权读取别人会话。
 建议：每次都校验 `conversation_id + user_id`。

## 风险 4：上下文无节制拼接

后果：成本高、延迟大、效果不稳定。
 建议：引入摘要和优先级控制。

## 风险 5：把敏感隐私直接长期注入 Prompt

后果：隐私泄露风险。
 建议：敏感信息脱敏、加密、最小化使用。