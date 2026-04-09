-- 银行卡管理后台系统 - 测试数据
-- 执行前请确保已执行 init.sql

USE bank_admin;

-- ===================== 系统用户 =====================
-- 密码均为: 123456 (BCrypt加密)
INSERT INTO `bank_sys_user` (`username`, `password`, `nickname`, `role`, `status`, `is_deleted`, `create_time`) VALUES
('zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', 'OPERATOR', 0, 0, NOW()),
('lisi', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四', 'VIEWER', 0, 0, NOW()),
('wangwu', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王五', 'VIEWER', 1, 0, NOW());

-- ===================== 持卡人 =====================
INSERT INTO `card_owner` (`id`, `name`, `phone`, `id_card`, `remark`, `status`, `is_deleted`, `create_time`) VALUES
(1, '张伟', '138****1234', '310***********1234', '主要持卡人', 0, 0, NOW()),
(2, '李娜', '139****5678', '310***********5678', '家庭成员', 0, 0, NOW()),
(3, '王芳', '137****9012', '310***********9012', '备用持卡人', 0, 0, NOW()),
(4, '赵强', '136****3456', '310***********3456', '商务用卡', 0, 0, NOW());

-- ===================== 银行卡 =====================
INSERT INTO `bank_card` (`id`, `owner_id`, `bank_name`, `card_no`, `card_no_last4`, `card_type`, `credit_limit`, `balance`, `used_amount`, `bill_day`, `repay_day`, `expire_date`, `status`, `is_deleted`, `create_time`) VALUES
-- 张伟的卡
(1, 1, '招商银行', '6225************1234', '1234', 2, 50000.00, 0.00, 12580.50, 5, 25, '2028-06', 0, 0, NOW()),
(2, 1, '工商银行', '6222************5678', '5678', 2, 30000.00, 0.00, 8500.00, 10, 30, '2027-12', 0, 0, NOW()),
(3, 1, '建设银行', '6217************9012', '9012', 1, NULL, 56800.25, 0.00, NULL, NULL, NULL, 0, 0, NOW()),

-- 李娜的卡
(4, 2, '浦发银行', '6226************3456', '3456', 2, 20000.00, 0.00, 3200.00, 15, 5, '2026-09', 0, 0, NOW()),
(5, 2, '农业银行', '6228************7890', '7890', 1, NULL, 12890.50, 0.00, NULL, NULL, NULL, 0, 0, NOW()),

-- 王芳的卡
(6, 3, '交通银行', '6222************2345', '2345', 2, 15000.00, 0.00, 6780.00, 20, 10, '2025-03', 0, 0, NOW()),

-- 赵强的卡
(7, 4, '中信银行', '6226************6789', '6789', 2, 100000.00, 0.00, 45600.00, 1, 20, '2029-01', 0, 0, NOW()),
(8, 4, '光大银行', '6225************0123', '0123', 2, 50000.00, 0.00, 12000.00, 8, 28, '2027-06', 0, 0, NOW());

-- ===================== 流水记录（近30天） =====================
INSERT INTO `card_transaction` (`card_id`, `owner_id`, `tx_type`, `amount`, `tx_date`, `description`, `counterpart`, `balance_snapshot`, `is_deleted`, `create_time`) VALUES
-- 招商银行信用卡流水
(1, 1, 2, 2580.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '京东商城购物', '京东', NULL, 0, NOW()),
(1, 1, 2, 350.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '美团外卖', '美团', NULL, 0, NOW()),
(1, 1, 2, 1280.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '淘宝购物', '淘宝', NULL, 0, NOW()),
(1, 1, 2, 89.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '滴滴出行', '滴滴', NULL, 0, NOW()),
(1, 1, 2, 450.00, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '超市购物', '沃尔玛', NULL, 0, NOW()),
(1, 1, 1, 5000.00, DATE_SUB(CURDATE(), INTERVAL 10 DAY), '工资转入', '公司', NULL, 0, NOW()),
(1, 1, 2, 1200.00, DATE_SUB(CURDATE(), INTERVAL 12 DAY), '餐饮消费', '海底捞', NULL, 0, NOW()),
(1, 1, 2, 680.00, DATE_SUB(CURDATE(), INTERVAL 15 DAY), '加油站', '中石化', NULL, 0, NOW()),

-- 工商银行信用卡流水
(2, 1, 2, 1500.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '商场购物', '银泰百货', NULL, 0, NOW()),
(2, 1, 2, 800.00, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '药店', '老百姓大药房', NULL, 0, NOW()),
(2, 1, 2, 220.00, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '星巴克', '星巴克', NULL, 0, NOW()),
(2, 1, 1, 3000.00, DATE_SUB(CURDATE(), INTERVAL 14 DAY), '报销款', '公司', NULL, 0, NOW()),

-- 建设银行借记卡流水
(3, 1, 1, 8500.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '工资', '公司', 56800.25, 0, NOW()),
(3, 1, 2, 3000.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '房租转账', '房东', 53800.25, 0, NOW()),
(3, 1, 2, 500.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 'ATM取款', 'ATM', 53300.25, 0, NOW()),

-- 浦发银行信用卡流水
(4, 2, 2, 680.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '网购', '拼多多', NULL, 0, NOW()),
(4, 2, 2, 150.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '电影票', '猫眼', NULL, 0, NOW()),
(4, 2, 2, 280.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '鲜花', '花店', NULL, 0, NOW()),

-- 交通银行信用卡流水
(6, 3, 2, 1200.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '家电维修', '维修店', NULL, 0, NOW()),
(6, 3, 2, 380.00, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '超市购物', '永辉超市', NULL, 0, NOW()),
(6, 3, 2, 560.00, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '药店', '大参林', NULL, 0, NOW()),

-- 中信银行信用卡流水
(7, 4, 2, 8500.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '酒店预订', '携程', NULL, 0, NOW()),
(7, 4, 2, 3200.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '机票', '携程', NULL, 0, NOW()),
(7, 4, 2, 1500.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '商务宴请', '酒店', NULL, 0, NOW()),
(7, 4, 2, 680.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '租车', '神州租车', NULL, 0, NOW()),
(7, 4, 1, 20000.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY), '差旅报销', '公司', NULL, 0, NOW()),

-- 光大银行信用卡流水
(8, 4, 2, 2800.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '办公用品', ' Staples', NULL, 0, NOW()),
(8, 4, 2, 450.00, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '快递费', '顺丰', NULL, 0, NOW()),
(8, 4, 2, 180.00, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '咖啡', '瑞幸', NULL, 0, NOW());

-- ===================== 账单记录 =====================
INSERT INTO `card_bill` (`card_id`, `owner_id`, `bill_month`, `bill_amount`, `min_pay_amount`, `repay_date`, `actual_pay_amount`, `actual_pay_date`, `status`, `is_deleted`, `create_time`) VALUES
-- 招商银行账单
(1, 1, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 9850.00, 985.00, LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) + INTERVAL 25 DAY, 9850.00, LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) + INTERVAL 23 DAY, 1, 0, NOW()),
(1, 1, DATE_FORMAT(CURDATE(), '%Y-%m'), 4999.50, 500.00, LAST_DAY(CURDATE()) + INTERVAL 25 DAY, NULL, NULL, 0, 0, NOW()),

-- 工商银行账单
(2, 1, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 6500.00, 650.00, LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) + INTERVAL 30 DAY, 6500.00, LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) + INTERVAL 28 DAY, 1, 0, NOW()),
(2, 1, DATE_FORMAT(CURDATE(), '%Y-%m'), 2520.00, 252.00, LAST_DAY(CURDATE()) + INTERVAL 30 DAY, NULL, NULL, 0, 0, NOW()),

-- 浦发银行账单
(4, 2, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 2100.00, 210.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 5 DAY), 2100.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 3 DAY), 1, 0, NOW()),
(4, 2, DATE_FORMAT(CURDATE(), '%Y-%m'), 1110.00, 111.00, DATE_ADD(LAST_DAY(CURDATE()), INTERVAL 5 DAY), NULL, NULL, 0, 0, NOW()),

-- 交通银行账单
(6, 3, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 4500.00, 450.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 10 DAY), 2000.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 8 DAY), 2, 0, NOW()),
(6, 3, DATE_FORMAT(CURDATE(), '%Y-%m'), 2140.00, 214.00, DATE_ADD(LAST_DAY(CURDATE()), INTERVAL 10 DAY), NULL, NULL, 0, 0, NOW()),

-- 中信银行账单
(7, 4, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 28000.00, 2800.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 20 DAY), 28000.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 18 DAY), 1, 0, NOW()),
(7, 4, DATE_FORMAT(CURDATE(), '%Y-%m'), 13880.00, 1388.00, DATE_ADD(LAST_DAY(CURDATE()), INTERVAL 20 DAY), NULL, NULL, 0, 0, NOW()),

-- 光大银行账单
(8, 4, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 8500.00, 850.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 28 DAY), 8500.00, DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL 25 DAY), 1, 0, NOW()),
(8, 4, DATE_FORMAT(CURDATE(), '%Y-%m'), 3430.00, 343.00, DATE_ADD(LAST_DAY(CURDATE()), INTERVAL 28 DAY), NULL, NULL, 0, 0, NOW());

-- ===================== 提醒任务 =====================
INSERT INTO `reminder_task` (`owner_id`, `card_id`, `bill_id`, `reminder_type`, `reminder_date`, `content`, `status`, `is_deleted`, `create_time`) VALUES
(1, 1, NULL, 4, DATE_ADD(CURDATE(), INTERVAL 30 DAY), '招商银行信用卡(尾号1234)将于2028-06到期，请及时换卡', 0, 0, NOW()),
(1, 2, NULL, 4, DATE_ADD(CURDATE(), INTERVAL 20 DAY), '工商银行信用卡(尾号5678)将于2027-12到期，请及时换卡', 0, 0, NOW()),
(2, 4, NULL, 4, DATE_SUB(CURDATE(), INTERVAL 180 DAY), '浦发银行信用卡(尾号3456)将于2026-09到期', 1, 0, NOW()),
(3, 6, NULL, 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '交通银行信用卡(尾号2345)账单即将到期，请及时还款', 0, 0, NOW()),
(1, 1, NULL, 1, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '招商银行信用卡(尾号1234)本月账单还款日即将到来', 0, 0, NOW()),
(4, 7, NULL, 1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '中信银行信用卡(尾号6789)本月账单还款日即将到来', 0, 0, NOW());

-- ===================== 操作日志 =====================
INSERT INTO `operation_log` (`operator`, `module`, `action`, `description`, `request_path`, `request_method`, `result`, `create_time`) VALUES
('admin', '系统', '登录', '管理员登录系统', '/api/auth/login', 'POST', 0, NOW()),
('admin', '持卡人', '新增', '新增持卡人：张伟', '/api/owners', 'POST', 0, NOW()),
('admin', '持卡人', '新增', '新增持卡人：李娜', '/api/owners', 'POST', 0, NOW()),
('admin', '银行卡', '新增', '新增银行卡：招商银行(1234)', '/api/cards', 'POST', 0, NOW()),
('zhangsan', '流水', '新增', '录入流水记录', '/api/transactions', 'POST', 0, NOW()),
('admin', '账单', '标记', '标记账单已还款', '/api/bills/1/pay', 'PUT', 0, NOW());
