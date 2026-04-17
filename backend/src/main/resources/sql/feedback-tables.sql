-- ============================================
-- 用户反馈管理模块 - 建表脚本
-- ============================================

USE bank_admin;

-- ===================== 1. 反馈主表 =====================
CREATE TABLE IF NOT EXISTS `user_feedback` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `feedback_no`      VARCHAR(32)   NOT NULL                COMMENT '反馈编号（唯一）',
  `title`            VARCHAR(200)  NOT NULL                COMMENT '问题标题',
  `content`          TEXT          NOT NULL                COMMENT '详细描述',
  `feedback_type`    TINYINT       NOT NULL DEFAULT 0      COMMENT '类型：0功能异常 1界面体验 2数据问题 3权限问题 4其他',
  `priority`         TINYINT       NOT NULL DEFAULT 1      COMMENT '优先级：0低 1中 2高 3紧急',
  `status`           TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0待处理 1修复中 2已解决 3已关闭',
  `submitter`        VARCHAR(64)   DEFAULT NULL            COMMENT '提交人',
  `assignee`         VARCHAR(64)   DEFAULT NULL            COMMENT '处理人',
  `latest_remark`    VARCHAR(500)  DEFAULT NULL            COMMENT '最近处理备注',
  `attachment_count` INT           NOT NULL DEFAULT 0      COMMENT '附件数量',
  `resolved_time`    DATETIME      DEFAULT NULL            COMMENT '解决时间',
  `closed_time`      DATETIME      DEFAULT NULL            COMMENT '关闭时间',
  `is_deleted`       TINYINT(1)    NOT NULL DEFAULT 0,
  `create_by`        VARCHAR(64)   DEFAULT NULL,
  `create_time`      DATETIME      DEFAULT NULL,
  `update_by`        VARCHAR(64)   DEFAULT NULL,
  `update_time`      DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feedback_no` (`feedback_no`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_submitter` (`submitter`),
  KEY `idx_create_time` (`create_time`),

  KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈主表';

-- ===================== 2. 反馈附件表 =====================
CREATE TABLE IF NOT EXISTS `user_feedback_attachment` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `feedback_id`  BIGINT        NOT NULL                COMMENT '关联反馈ID',
  `file_name`    VARCHAR(255)  NOT NULL                COMMENT '原始文件名',
  `file_url`     VARCHAR(500)  NOT NULL                COMMENT '文件访问地址',
  `file_path`    VARCHAR(500)  NOT NULL                COMMENT '服务器存储路径',
  `file_size`    BIGINT        NOT NULL DEFAULT 0      COMMENT '文件大小（字节）',
  `file_suffix`  VARCHAR(20)   DEFAULT NULL            COMMENT '文件后缀（小写，不含点）',
  `content_type` VARCHAR(100)  DEFAULT NULL            COMMENT 'MIME类型',
  `sort_no`      INT           NOT NULL DEFAULT 0      COMMENT '排序',
  `is_deleted`   TINYINT(1)    NOT NULL DEFAULT 0,
  `create_by`    VARCHAR(64)   DEFAULT NULL,
  `create_time`  DATETIME      DEFAULT NULL,
  `update_by`    VARCHAR(64)   DEFAULT NULL,
  `update_time`  DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_id` (`feedback_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈附件表';

-- ===================== 3. 反馈处理轨迹表 =====================
CREATE TABLE IF NOT EXISTS `user_feedback_process_log` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `feedback_id`  BIGINT        NOT NULL                COMMENT '关联反馈ID',
  `action_type`  VARCHAR(32)   NOT NULL                COMMENT '动作类型：SUBMIT/STATUS_CHANGE/REMARK/ATTACHMENT/ASSIGN/CLOSE',
  `from_status`  TINYINT       DEFAULT NULL            COMMENT '变更前状态',
  `to_status`    TINYINT       DEFAULT NULL            COMMENT '变更后状态',
  `remark`       VARCHAR(1000) DEFAULT NULL            COMMENT '备注内容',
  `operator`     VARCHAR(64)   NOT NULL                COMMENT '操作人',
  `operate_time` DATETIME      NOT NULL                COMMENT '操作时间',
  `is_deleted`   TINYINT(1)    NOT NULL DEFAULT 0,
  `create_by`    VARCHAR(64)   DEFAULT NULL,
  `create_time`  DATETIME      DEFAULT NULL,
  `update_by`    VARCHAR(64)   DEFAULT NULL,
  `update_time`  DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_id` (`feedback_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈处理轨迹表';
