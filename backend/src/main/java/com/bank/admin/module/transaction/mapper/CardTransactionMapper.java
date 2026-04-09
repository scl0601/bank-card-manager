package com.bank.admin.module.transaction.mapper;

import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.vo.CardTransactionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Map;

/**
 * 流水 Mapper
 */
@Mapper
public interface CardTransactionMapper extends BaseMapper<CardTransaction> {

    IPage<CardTransactionVO> selectPageWithInfo(
            Page<CardTransactionVO> page,
            @Param("cardId") Long cardId,
            @Param("ownerId") Long ownerId,
            @Param("txType") Integer txType,
            @Param("txDateStart") LocalDate txDateStart,
            @Param("txDateEnd") LocalDate txDateEnd
    );

    /** 按卡统计收支 */
    Map<String, Object> sumByCard(@Param("cardId") Long cardId);
}
