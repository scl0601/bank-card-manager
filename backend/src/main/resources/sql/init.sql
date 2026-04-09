-- 银行卡管理后台系统 - 初始化SQL
-- 执行顺序：建库 → 建表 → 初始数据

CREATE DATABASE IF NOT EXISTS bank_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bank_admin;

-- ===================== 系统用户表 =====================
CREATE TABLE `bank_sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(64)  NOT NULL                COMMENT '用户名',
  `password`    VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt）',
  `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  `role`        VARCHAR(32)  NOT NULL DEFAULT 'VIEWER' COMMENT '角色：ADMIN/OPERATOR/VIEWER',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0正常 1禁用',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ===================== 持卡人表 =====================
CREATE TABLE `card_owner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64)  NOT NULL                COMMENT '姓名',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `id_card`     VARCHAR(255) DEFAULT NULL            COMMENT '身份证号',
  `remark`      VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0正常 1禁用',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持卡人表';

-- ===================== 银行卡表 =====================
CREATE TABLE `bank_card` (
  `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_id`        BIGINT          NOT NULL                COMMENT '持卡人ID',
  `bank_name`       VARCHAR(64)     NOT NULL                COMMENT '银行名称',
  `card_no`         VARCHAR(255)    NOT NULL                COMMENT '卡号（加密）',
  `card_no_last4`   CHAR(4)         NOT NULL                COMMENT '卡号后四位',
  `card_type`       TINYINT         NOT NULL DEFAULT 1      COMMENT '1借记卡 2信用卡',
  `credit_limit`    DECIMAL(18,2)   DEFAULT NULL            COMMENT '信用额度',
  `balance`         DECIMAL(18,2)   DEFAULT 0.00            COMMENT '当前余额',
  `used_amount`     DECIMAL(18,2)   DEFAULT 0.00            COMMENT '已用额度',
  `bill_day`        TINYINT         DEFAULT NULL            COMMENT '账单日',
  `repay_day`       TINYINT         DEFAULT NULL            COMMENT '还款日',
  `expire_date`     VARCHAR(7)      DEFAULT NULL            COMMENT '有效期（yyyy-MM）',
  `status`          TINYINT         NOT NULL DEFAULT 0      COMMENT '0正常 1冻结 2注销',
  `remark`          VARCHAR(500)    DEFAULT NULL,
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`       VARCHAR(64)     DEFAULT NULL,
  `create_time`     DATETIME        DEFAULT NULL,
  `update_by`       VARCHAR(64)     DEFAULT NULL,
  `update_time`     DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id`    (`owner_id`),
  KEY `idx_card_type`   (`card_type`),
  KEY `idx_status`      (`status`),
  KEY `idx_expire_date` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行卡表';

-- ===================== 流水表 =====================
CREATE TABLE `card_transaction` (
  `id`               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `card_id`          BIGINT          NOT NULL                COMMENT '银行卡ID',
  `owner_id`         BIGINT          NOT NULL                COMMENT '持卡人ID',
  `tx_type`          TINYINT         NOT NULL                COMMENT '1收入 2支出',
  `amount`           DECIMAL(18,2)   NOT NULL                COMMENT '交易金额',
  `tx_date`          DATE            NOT NULL                COMMENT '交易日期',
  `description`      VARCHAR(255)    DEFAULT NULL            COMMENT '交易描述',
  `counterpart`      VARCHAR(255)    DEFAULT NULL            COMMENT '对手方',
  `balance_snapshot` DECIMAL(18,2)   DEFAULT NULL            COMMENT '余额快照',
  `remark`           VARCHAR(500)    DEFAULT NULL,
  `is_deleted`       TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`        VARCHAR(64)     DEFAULT NULL,
  `create_time`      DATETIME        DEFAULT NULL,
  `update_by`        VARCHAR(64)     DEFAULT NULL,
  `update_time`      DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_card_id`  (`card_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_tx_date`  (`tx_date`),
  KEY `idx_tx_type`  (`tx_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水记录表';

-- ===================== 账单表 =====================
CREATE TABLE `card_bill` (
  `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `card_id`           BIGINT          NOT NULL                COMMENT '银行卡ID',
  `owner_id`          BIGINT          NOT NULL                COMMENT '持卡人ID',
  `bill_month`        VARCHAR(7)      NOT NULL                COMMENT '账单月份（yyyy-MM）',
  `bill_amount`       DECIMAL(18,2)   NOT NULL                COMMENT '账单金额',
  `min_pay_amount`    DECIMAL(18,2)   DEFAULT NULL            COMMENT '最低还款额',
  `repay_date`        DATE            DEFAULT NULL            COMMENT '还款日',
  `actual_pay_amount` DECIMAL(18,2)   DEFAULT NULL            COMMENT '实际还款金额',
  `actual_pay_date`   DATE            DEFAULT NULL            COMMENT '实际还款日期',
  `status`            TINYINT         NOT NULL DEFAULT 0      COMMENT '0待还款 1已还清 2部分还款 3逾期',
  `remark`            VARCHAR(500)    DEFAULT NULL,
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`         VARCHAR(64)     DEFAULT NULL,
  `create_time`       DATETIME        DEFAULT NULL,
  `update_by`         VARCHAR(64)     DEFAULT NULL,
  `update_time`       DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_card_id`    (`card_id`),
  KEY `idx_owner_id`   (`owner_id`),
  KEY `idx_bill_month` (`bill_month`),
  KEY `idx_status`     (`status`),
  KEY `idx_repay_date` (`repay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

-- ===================== 提醒任务表 =====================
CREATE TABLE `reminder_task` (
  `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_id`      BIGINT          NOT NULL                COMMENT '持卡人ID',
  `card_id`       BIGINT          DEFAULT NULL            COMMENT '银行卡ID',
  `bill_id`       BIGINT          DEFAULT NULL            COMMENT '账单ID',
  `reminder_type` TINYINT         NOT NULL                COMMENT '1即将到期 2今日到期 3已逾期 4卡片即将过期',
  `reminder_date` DATE            NOT NULL                COMMENT '提醒日期',
  `content`       VARCHAR(500)    NOT NULL                COMMENT '提醒内容',
  `status`        TINYINT         NOT NULL DEFAULT 0      COMMENT '0待处理 1已处理 2已忽略',
  `handled_time`  DATETIME        DEFAULT NULL,
  `handled_by`    VARCHAR(64)     DEFAULT NULL,
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`     VARCHAR(64)     DEFAULT NULL,
  `create_time`   DATETIME        DEFAULT NULL,
  `update_by`     VARCHAR(64)     DEFAULT NULL,
  `update_time`   DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id`      (`owner_id`),
  KEY `idx_reminder_date` (`reminder_date`),
  KEY `idx_status`        (`status`),
  KEY `idx_type_status`   (`reminder_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒任务表';

-- ===================== 操作日志表 =====================
CREATE TABLE `operation_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator`       VARCHAR(64)   DEFAULT NULL COMMENT '操作人',
  `module`         VARCHAR(64)   DEFAULT NULL COMMENT '操作模块',
  `action`         VARCHAR(64)   DEFAULT NULL COMMENT '操作类型',
  `description`    VARCHAR(255)  DEFAULT NULL COMMENT '操作描述',
  `request_path`   VARCHAR(255)  DEFAULT NULL COMMENT '请求路径',
  `request_method` VARCHAR(16)   DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT          DEFAULT NULL COMMENT '请求参数',
  `result`         TINYINT       DEFAULT 0    COMMENT '0成功 1失败',
  `error_msg`      VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
  `client_ip`      VARCHAR(64)   DEFAULT NULL COMMENT '客户端IP',
  `duration`       BIGINT        DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time`    DATETIME      DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator`    (`operator`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ===================== 初始管理员账号 =====================
-- 密码：admin123（BCrypt加密）
INSERT INTO `bank_sys_user` (`username`, `password`, `nickname`, `role`, `status`, `is_deleted`, `create_time`)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 'ADMIN', 0, 0, NOW());

-- ===================== 记账分类表 =====================
CREATE TABLE `book_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64)  NOT NULL                COMMENT '分类名称',
  `icon`        VARCHAR(32)  DEFAULT NULL            COMMENT '图标标识',
  `type`        TINYINT      NOT NULL                COMMENT '1收入 2支出',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父级ID（0=一级）',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '0启用 1停用',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账分类表';

-- ===================== 个人记账表 =====================
CREATE TABLE `personal_book` (
  `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_date`   DATE            NOT NULL                COMMENT '记账日期',
  `book_type`   TINYINT         NOT NULL                COMMENT '1收入 2支出',
  `amount`      DECIMAL(18,2)   NOT NULL                COMMENT '金额',
  `category_id` BIGINT          NOT NULL                COMMENT '分类ID',
  `description` VARCHAR(255)    DEFAULT NULL            COMMENT '描述/备注',
  `card_id`     BIGINT          DEFAULT NULL            COMMENT '关联银行卡ID（可选）',
  `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)     DEFAULT NULL,
  `create_time` DATETIME        DEFAULT NULL,
  `update_by`   VARCHAR(64)     DEFAULT NULL,
  `update_time` DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_book_date` (`book_date`),
  KEY `idx_book_type` (`book_type`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_card_id` (`card_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人记账表';

-- ===================== 默认分类数据 =====================
-- 收入分类
INSERT INTO `book_category` (`name`, `icon`, `type`, `sort_order`, `parent_id`, `status`, `create_time`) VALUES
('工资',     'Money', 1, 1, 0, 0, NOW()),
('奖金',     'Trophy', 1, 2, 0, 0, NOW()),
('兼职',     'Briefcase', 1, 3, 0, 0, NOW()),
('投资理财', 'TrendCharts', 1, 4, 0, 0, NOW()),
('其他收入', 'MoreFilled', 1, 99, 0, 0, NOW());

-- 支出一级分类
INSERT INTO `book_category` (`name`, `icon`, `type`, `sort_order`, `parent_id`, `status`, `create_time`) VALUES
('餐饮',     'Food',     2, 1, 0, 0, NOW()),
('交通',     'Van',      2, 2, 0, 0, NOW()),
('购物',     'ShoppingBag', 2, 3, 0, 0, NOW()),
('居住',     'House',    2, 4, 0, 0, NOW()),
('医疗',     'FirstAidKit', 2, 5, 0, 0, NOW()),
('教育',     'Reading',  2, 6, 0, 0, NOW()),
('娱乐',     'Film',     2, 7, 0, 0, NOW()),
('人情',     'Present',  2, 8, 0, 0, NOW()),
('其他支出', 'MoreFilled', 2, 99, 0, 0, NOW());

-- 支出二级分类
INSERT INTO `book_category` (`name`, `icon`, `type`, `sort_order`, `parent_id`, `status`, `create_time`) VALUES
-- 餐饮
('早餐',     null, 2, 1, 6, 0, NOW()),
('午餐',     null, 2, 2, 6, 0, NOW()),
('晚餐',     null, 2, 3, 6, 0, NOW()),
('零食饮料', null, 2, 4, 6, 0, NOW()),
-- 交通
('公交地铁', null, 2, 1, 7, 0, NOW()),
('打车',     null, 2, 2, 7, 0, NOW()),
('停车费',   null, 2, 3, 7, 0, NOW()),
-- 购物
('日用品',   null, 2, 1, 8, 0, NOW()),
('服装鞋包', null, 2, 2, 8, 0, NOW()),
('电子产品', null, 2, 3, 8, 0, NOW()),
-- 居住
('房租',     null, 2, 1, 9, 0, NOW()),
('水电燃气', null, 2, 2, 9, 0, NOW()),
('网费',     null, 2, 3, 9, 0, NOW()),
('物业',     null, 2, 4, 9, 0, NOW());

-- ===================== 日程事项表 =====================
CREATE TABLE `calendar_event` (
  `id`               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`            VARCHAR(100)    NOT NULL                COMMENT '标题',
  `description`      VARCHAR(500)    DEFAULT NULL            COMMENT '描述内容',
  `event_date`       DATE            NOT NULL                COMMENT '事项日期',
  `start_time`       TIME            DEFAULT NULL            COMMENT '开始时间',
  `end_time`         TIME            DEFAULT NULL            COMMENT '结束时间',
  `category`         TINYINT         NOT NULL DEFAULT 0      COMMENT '分类：0工作 1生活 2学习 3健康 4其他',
  `priority`         TINYINT         NOT NULL DEFAULT 1      COMMENT '优先级：0低 1中 2高',
  `status`           TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0待办 1进行中 2已完成 3已取消',
  `remind_before_min`INT             DEFAULT NULL            COMMENT '提前提醒分钟数（null=不提醒）',
  `remark`           VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
  `is_deleted`       TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`        VARCHAR(64)     DEFAULT NULL,
  `create_time`      DATETIME        DEFAULT NULL,
  `update_by`        VARCHAR(64)     DEFAULT NULL,
  `update_time`      DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event_date`  (`event_date`),
  KEY `idx_category`    (`category`),
  KEY `idx_status`      (`status`),
  KEY `idx_date_status` (`event_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日程事项表';

-- ===================== 日程事项模拟数据 =====================
INSERT INTO `calendar_event` (`title`, `description`, `event_date`, `start_time`, `end_time`, `category`, `priority`, `status`, `remind_before_min`, `remark`, `is_deleted`, `create_by`, `create_time`) VALUES
-- ====== 4月7日（周二）======
('晨会', '每日站会，汇报昨日进度和今日计划', '2026-04-07', '09:00:00', '09:30:00', 0, 1, 2, 15, NULL, 0, 'admin', NOW()),
('代码审查 - 用户模块PR', '审查张三提交的用户模块重构PR', '2026-04-07', '10:00:00', '11:30:00', 0, 2, 2, 30, NULL, 0, 'admin', NOW()),
('午餐', NULL, '2026-04-07', '12:00:00', '13:00:00', 1, 0, 2, NULL, NULL, 0, 'admin', NOW()),
('阅读《系统设计面试》第5章', NULL, '2026-04-07', '20:00:00', '21:30:00', 2, 0, 2, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月8日（周三）======
('产品需求评审会议', '讨论Q2新功能的PRD文档，需要准备问题清单', '2026-04-08', '09:30:00', '11:00:00', 0, 2, 2, 30, '带好笔记本', 0, 'admin', NOW()),
('修复登录页样式兼容性问题', 'Safari浏览器下按钮位置偏移', '2026-04-08', '14:00:00', '15:30:00', 0, 2, 2, NULL, NULL, 0, 'admin', NOW()),
('团队周报撰写', '汇总本周完成的工作内容', '2026-04-08', '16:00:00', '17:00:00', 0, 1, 2, NULL, NULL, 0, 'admin', NOW()),
('健身 - 跑步5公里', NULL, '2026-04-08', '19:00:00', '20:00:00', 3, 0, 2, NULL, '户外跑', 0, 'admin', NOW()),

-- ====== 4月9日（周四 - 今天）======
('团队周会', '每周四固定周会，汇报本周进度', '2026-04-09', '10:00:00', '11:00:00', 0, 2, 0, 15, '准备PPT', 0, 'admin', NOW()),
('审批银行卡申请流程测试', '测试新增的银行卡审批流程是否正常', '2026-04-09', '14:00:00', '16:00:00', 0, 2, 1, 30, '重点测试异常流程', 0, 'admin', NOW()),
('代码开发 - 流水模块优化', '优化流水列表查询性能，添加索引', '2026-04-09', '16:30:00', '18:00:00', 0, 1, 0, NULL, NULL, 0, 'admin', NOW()),
('晚餐 - 和朋友聚餐', '老同学聚会，海底捞', '2026-04-09', '18:30:00', '21:00:00', 1, 0, 0, 60, NULL, 0, 'admin', NOW()),
('学习 Vue 3 Composition API 进阶', '观看教程视频并做笔记', '2026-04-09', '21:30:00', '22:30:00', 2, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('缴纳水电燃气费', NULL, '2026-04-09', '20:00:00', '20:15:00', 4, 1, 0, 1440, '别忘了！', 0, 'admin', NOW()),

-- ====== 4月10日（周五）======
('技术分享 - MyBatis-Plus最佳实践', '给团队成员做技术分享', '2026-04-10', '10:00:00', '11:30:00', 0, 2, 0, 60, '准备PPT和Demo', 0, 'admin', NOW()),
('联调接口 - 日历计划模块', '前后端联调日历计划的CRUD接口', '2026-04-10', '14:00:00', '17:00:00', 0, 2, 0, NULL, NULL, 0, 'admin', NOW()),
('瑜伽课', '每周五晚上的瑜伽课', '2026-04-10', '19:00:00', '20:30:00', 3, 1, 0, 120, '带瑜伽垫', 0, 'admin', NOW()),
('看电影', '《沙丘2》IMAX版', '2026-04-10', '21:00:00', '23:30:00', 1, 0, 0, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月11日（周六）======
('睡懒觉', '难得的周末，好好休息', '2026-04-11', '09:00:00', '10:00:00', 1, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('超市采购', '买一周的食材和生活用品', '2026-04-11', '10:30:00', '12:00:00', 1, 1, 0, NULL, '列好购物清单', 0, 'admin', NOW()),
('整理房间', '大扫除，整理书架和衣柜', '2026-04-11', '14:00:00', '17:00:00', 1, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('学习英语 - 雅思阅读练习', NULL, '2026-04-11', '19:00:00', '20:30:00', 2, 1, 0, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月12日（周日）======
('晨跑 - 公园10公里', NULL, '2026-04-12', '06:30:00', '08:00:00', 3, 0, 0, NULL, '天气好的话去户外跑', 0, 'admin', NOW()),
('做饭 - 尝试新菜谱', '红烧肉 + 清炒时蔬', '2026-04-12', '11:00:00', '13:00:00', 1, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('阅读《深入理解计算机系统》第三章', NULL, '2026-04-12', '15:00:00', '17:00:00', 2, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('看纪录片 - 《地球脉动》第三季', NULL, '2026-04-12', '20:00:00', '22:00:00', 1, 0, 0, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月13日（周一）======
('周报发送', '将上周周报发给领导', '2026-04-13', '09:00:00', '09:15:00', 0, 2, 0, 30, NULL, 0, 'admin', NOW()),
('需求排期会', '讨论下周的开发任务分配', '2026-04-13', '10:00:00', '11:00:00', 0, 1, 0, 15, NULL, 0, 'admin', NOW()),
('编写技术方案文档 - 提醒模块优化', NULL, '2026-04-13', '14:00:00', '17:00:00', 0, 1, 0, NULL, NULL, 0, 'admin', NOW()),
('健身房 - 力量训练', '胸肌+三头肌训练日', '2026-04-13', '19:00:00', '20:30:00', 3, 0, 0, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月14日（周二）======
('晨会', NULL, '2026-04-14', '09:00:00', '09:30:00', 0, 1, 0, 15, NULL, 0, 'admin', NOW()),
('代码开发 - 账单统计功能', '实现账单月度统计图表接口', '2026-04-14', '10:00:00', '12:00:00', 0, 2, 0, NULL, NULL, 0, 'admin', NOW()),
('午休', NULL, '2026-04-14', '12:30:00', '13:30:00', 1, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('Bug修复 - 导出Excel乱码问题', NULL, '2026-04-14', '14:00:00', '15:00:00', 0, 2, 0, NULL, NULL, 0, 'admin', NOW()),
('在线课程 - Spring Boot微服务架构', NULL, '2026-04-14', '20:00:00', '21:30:00', 2, 1, 0, NULL, '第8讲：服务注册发现', 0, 'admin', NOW()),

-- ====== 4月15日（周三）======
('季度OKR复盘会', 'Q1 OKR结果回顾和Q2目标制定', '2026-04-15', '09:30:00', '12:00:00', 0, 2, 0, 60, '准备好数据', 0, 'admin', NOW()),
('新员工培训 - 后端规范讲解', '给新入职同事讲解后端开发规范', '2026-04-15', '14:00:00', '15:30:00', 0, 1, 0, 30, '准备培训材料', 0, 'admin', NOW()),
('代码Review - 记账模块', NULL, '2026-04-15', '16:00:00', '17:00:00', 0, 1, 0, NULL, NULL, 0, 'admin', NOW()),
('羽毛球 - 公司社团活动', NULL, '2026-04-15', '18:30:00', '20:00:00', 3, 0, 0, 120, '带球拍和水', 0, 'admin', NOW()),

-- ====== 4月16日（周四）======
('团队周会', NULL, '2026-04-16', '10:00:00', '11:00:00', 0, 1, 0, 15, NULL, 0, 'admin', NOW()),
('数据库性能优化', '分析慢SQL并添加索引', '2026-04-16', '14:00:00', '17:00:00', 0, 2, 0, NULL, NULL, 0, 'admin', NOW()),
('体检预约提醒', '年度体检时间是下周三上午', '2026-04-16', '20:00:00', '20:05:00', 4, 1, 0, 1440, '记得空腹！', 0, 'admin', NOW()),

-- ====== 4月17日（周五）======
('项目上线部署', 'V2.0版本正式上线到生产环境', '2026-04-17', '10:00:00', '12:00:00', 0, 2, 0, 60, '提前准备回滚方案', 0, 'admin', NOW()),
('上线后监控与验证', '检查各模块运行状态，确认无异常', '2026-04-17', '14:00:00', '16:00:00', 0, 2, 0, NULL, NULL, 0, 'admin', NOW()),
('庆祝聚餐 - 项目成功上线', '团队聚餐庆祝', '2026-04-17', '18:30:00', '21:00:00', 1, 0, 0, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月18日（周六）======
('图书馆自习', '去市图书馆看书学习一整天', '2026-04-18', '09:00:00', '17:00:00', 2, 1, 0, NULL, '带笔记本和充电器', 0, 'admin', NOW()),
('游泳馆 - 自由泳1000米', NULL, '2026-04-18', '18:00:00', '19:30:00', 3, 0, 0, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月19日（周日）======
('家庭视频通话', '和父母视频聊天', '2026-04-19', '10:00:00', '11:00:00', 1, 1, 0, 30, NULL, 0, 'admin', NOW()),
('整理照片 - 手机相册备份', '整理最近的照片并备份到云盘', '2026-04-19', '15:00:00', '16:30:00', 4, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('学习 Rust 语言基础', '继续Rust入门教程第6章', '2026-04-19', '20:00:00', '21:30:00', 2, 0, 0, NULL, NULL, 0, 'admin', NOW()),

-- ====== 4月20日（周一） - 一些已取消的事项 ======
('原定：客户现场演示（已改期）', NULL, '2026-04-20', '10:00:00', '12:00:00', 0, 2, 3, NULL, '客户临时有事，改到下周五', 0, 'admin', NOW()),
('牙医复诊（已取消）', NULL, '2026-04-20', '15:00:00', '16:00:00', 4, 1, 3, 1440, '诊所装修暂停营业', 0, 'admin', NOW()),

-- ====== 4月21日~月底 少量分散数据 ======
('月度总结报告撰写', '整理本月所有工作产出', '2026-04-25', '09:00:00', '11:00:00', 0, 2, 0, NULL, NULL, 0, 'admin', NOW()),
('学习 Docker 容器化部署', '学习Docker Compose编排多容器应用', '2026-04-26', '20:00:00', '21:30:00', 2, 0, 0, NULL, NULL, 0, 'admin', NOW()),
('更换空调滤网', NULL, '2026-04-27', '10:00:00', '10:30:00', 4, 0, 0, 1440, '买新的滤网', 0, 'admin', NOW()),
('长跑训练 - 半马配速练习', NULL, '2026-04-27', '06:00:00', '07:30:00', 3, 1, 0, NULL, '目标配速5:30/km', 0, 'admin', NOW()),
('月末盘点 - 个人记账对账', NULL, '2026-04-30', '20:00:00', '21:00:00', 1, 1, 0, NULL, '核对所有支出记录', 0, 'admin', NOW());
