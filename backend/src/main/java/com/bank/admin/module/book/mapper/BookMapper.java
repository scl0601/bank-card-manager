package com.bank.admin.module.book.mapper;

import com.bank.admin.module.book.entity.PersonalBook;
import com.bank.admin.module.book.vo.BookVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 记账 Mapper
 */
@Mapper
public interface BookMapper extends BaseMapper<PersonalBook> {

    IPage<BookVO> selectPageWithInfo(
            Page<BookVO> page,
            @Param("bookType") Integer bookType,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("cardId") Long cardId,
            @Param("bookDateStart") LocalDate bookDateStart,
            @Param("bookDateEnd") LocalDate bookDateEnd
    );

    /** 按月份统计收支汇总 */
    Map<String, Object> sumByMonth(@Param("yearMonth") String yearMonth);
}
