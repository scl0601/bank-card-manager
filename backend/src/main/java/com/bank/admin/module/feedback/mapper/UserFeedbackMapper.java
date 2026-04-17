package com.bank.admin.module.feedback.mapper;

import com.bank.admin.module.feedback.entity.UserFeedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户反馈 Mapper
 */
@Mapper
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
}
