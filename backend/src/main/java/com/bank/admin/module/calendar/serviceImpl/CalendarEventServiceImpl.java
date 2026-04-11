package com.bank.admin.module.calendar.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.module.calendar.dto.CalendarEventQueryDTO;
import com.bank.admin.module.calendar.dto.CalendarEventSaveDTO;
import com.bank.admin.module.calendar.entity.CalendarEvent;
import com.bank.admin.module.calendar.mapper.CalendarEventMapper;
import com.bank.admin.module.calendar.service.CalendarEventService;
import com.bank.admin.module.calendar.vo.CalendarEventVO;
import com.bank.admin.module.calendar.vo.CalendarStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventMapper calendarEventMapper;

    /** 分类映射 */
    private static final Map<Integer, String> CATEGORY_MAP = Map.of(
            0, "工作",
            1, "生活",
            2, "学习",
            3, "健康",
            4, "其他"
    );

    @Override
    public List<CalendarEventVO> listByMonth(String month) {
        LambdaQueryWrapper<CalendarEvent> wrapper = Wrappers.<CalendarEvent>lambdaQuery()
                .likeRight(CalendarEvent::getEventDate, month)
                .orderByAsc(CalendarEvent::getEventDate)
                .orderByAsc(CalendarEvent::getStartTime);
        List<CalendarEvent> events = calendarEventMapper.selectList(wrapper);
        return convertToVOList(events);
    }

    @Override
    public List<CalendarEventVO> listByDay(LocalDate eventDate, Integer category, Integer status) {
        LambdaQueryWrapper<CalendarEvent> wrapper = Wrappers.<CalendarEvent>lambdaQuery()
                .eq(CalendarEvent::getEventDate, eventDate)
                .eq(category != null, CalendarEvent::getCategory, category)
                .eq(status != null, CalendarEvent::getStatus, status)
                .orderByAsc(CalendarEvent::getStartTime);
        List<CalendarEvent> events = calendarEventMapper.selectList(wrapper);
        return convertToVOList(events);
    }

    @Override
    public CalendarEventVO getDetail(Long id) {
        CalendarEvent entity = calendarEventMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "事项不存在");
        }
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(CalendarEventSaveDTO dto) {
        CalendarEvent entity = new CalendarEvent();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getPriority() == null) {
            entity.setPriority(1);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        calendarEventMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CalendarEventSaveDTO dto) {
        CalendarEvent exist = calendarEventMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "事项不存在");
        }
        CalendarEvent entity = new CalendarEvent();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        calendarEventMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CalendarEvent exist = calendarEventMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "事项不存在");
        }
        calendarEventMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        CalendarEvent exist = calendarEventMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "事项不存在");
        }
        CalendarEvent updateEntity = new CalendarEvent();
        updateEntity.setId(id);
        updateEntity.setStatus(status);
        calendarEventMapper.updateById(updateEntity);
    }

    @Override
    public CalendarStatsVO getMonthStats(String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 待办 (status=0)
        long todoCount = calendarEventMapper.selectCount(Wrappers.<CalendarEvent>lambdaQuery()
                .between(CalendarEvent::getEventDate, start, end)
                .eq(CalendarEvent::getStatus, 0));
        // 进行中 (status=1)
        long doingCount = calendarEventMapper.selectCount(Wrappers.<CalendarEvent>lambdaQuery()
                .between(CalendarEvent::getEventDate, start, end)
                .eq(CalendarEvent::getStatus, 1));
        // 已完成 (status=2)
        long doneCount = calendarEventMapper.selectCount(Wrappers.<CalendarEvent>lambdaQuery()
                .between(CalendarEvent::getEventDate, start, end)
                .eq(CalendarEvent::getStatus, 2));
        // 已取消 (status=3)
        long cancelledCount = calendarEventMapper.selectCount(Wrappers.<CalendarEvent>lambdaQuery()
                .between(CalendarEvent::getEventDate, start, end)
                .eq(CalendarEvent::getStatus, 3));

        CalendarStatsVO stats = new CalendarStatsVO();
        stats.setTodoCount(todoCount);
        stats.setDoingCount(doingCount);
        stats.setDoneCount(doneCount);
        stats.setCancelledCount(cancelledCount);
        return stats;
    }

    // ==================== 私有方法 ====================

    private List<CalendarEventVO> convertToVOList(List<CalendarEvent> entities) {
        return entities.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private CalendarEventVO convertToVO(CalendarEvent entity) {
        CalendarEventVO vo = new CalendarEventVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setCategoryName(CATEGORY_MAP.getOrDefault(entity.getCategory(), "其他"));
        return vo;
    }
}
