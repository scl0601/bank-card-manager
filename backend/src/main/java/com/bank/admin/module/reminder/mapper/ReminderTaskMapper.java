package com.bank.admin.module.reminder.mapper;

import com.bank.admin.module.reminder.dto.ReminderTaskQueryDTO;
import com.bank.admin.module.reminder.entity.ReminderTask;
import com.bank.admin.module.reminder.vo.ReminderTaskVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 提醒任务 Mapper
 */
@Mapper
public interface ReminderTaskMapper extends BaseMapper<ReminderTask> {

    /**
     * 分页查询（关联持卡人姓名、卡号后四位）
     */
    IPage<ReminderTaskVO> selectPageWithInfo(
            Page<ReminderTaskVO> page,
            @Param("query") ReminderTaskQueryDTO query
    );
}
