package com.bank.admin.module.announcement.service.impl;

import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.common.util.CurrentUserUtil;
import com.bank.admin.module.announcement.dto.AnnouncementQueryDTO;
import com.bank.admin.module.announcement.dto.AnnouncementSaveDTO;
import com.bank.admin.module.announcement.entity.SystemAnnouncement;
import com.bank.admin.module.announcement.entity.SystemAnnouncementUserState;
import com.bank.admin.module.announcement.mapper.SystemAnnouncementMapper;
import com.bank.admin.module.announcement.mapper.SystemAnnouncementUserStateMapper;
import com.bank.admin.module.announcement.service.SystemAnnouncementService;
import com.bank.admin.module.announcement.vo.AnnouncementVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemAnnouncementServiceImpl
        extends ServiceImpl<SystemAnnouncementMapper, SystemAnnouncement>
        implements SystemAnnouncementService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;

    private final SystemAnnouncementUserStateMapper stateMapper;

    @Override
    public PageResult<AnnouncementVO> adminPage(AnnouncementQueryDTO query) {
        LambdaQueryWrapper<SystemAnnouncement> wrapper = new LambdaQueryWrapper<SystemAnnouncement>()
                .like(StringUtils.hasText(query.getContent()), SystemAnnouncement::getContent, query.getContent())
                .eq(query.getStatus() != null, SystemAnnouncement::getStatus, query.getStatus())
                .orderByDesc(SystemAnnouncement::getPinned)
                .orderByDesc(SystemAnnouncement::getSortOrder)
                .orderByDesc(SystemAnnouncement::getPublishTime)
                .orderByDesc(SystemAnnouncement::getId);

        Page<SystemAnnouncement> page = page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return toPageResult(page, Map.of(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AnnouncementSaveDTO dto) {
        SystemAnnouncement announcement = new SystemAnnouncement();
        fillByDto(announcement, dto);
        announcement.setStatus(STATUS_DRAFT);
        announcement.setIsDeleted(0);
        save(announcement);
        return announcement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AnnouncementSaveDTO dto) {
        SystemAnnouncement announcement = getExisting(id);
        fillByDto(announcement, dto);
        updateById(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        SystemAnnouncement announcement = getExisting(id);
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.now());
        updateById(announcement);
        resetUserStateForRepublish(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        SystemAnnouncement announcement = getExisting(id);
        announcement.setStatus(STATUS_OFFLINE);
        updateById(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    @Override
    public AnnouncementVO latest() {
        SystemAnnouncement announcement = lambdaQuery()
                .eq(SystemAnnouncement::getStatus, STATUS_PUBLISHED)
                .orderByDesc(SystemAnnouncement::getPinned)
                .orderByDesc(SystemAnnouncement::getSortOrder)
                .orderByDesc(SystemAnnouncement::getPublishTime)
                .orderByDesc(SystemAnnouncement::getId)
                .last("LIMIT 1")
                .one();
        if (announcement == null) {
            return null;
        }
        SystemAnnouncementUserState state = getState(announcement.getId(), currentUsername());
        return toVO(announcement, state, true);
    }

    @Override
    public PageResult<AnnouncementVO> history(AnnouncementQueryDTO query) {
        LambdaQueryWrapper<SystemAnnouncement> wrapper = new LambdaQueryWrapper<SystemAnnouncement>()
                .eq(SystemAnnouncement::getStatus, STATUS_PUBLISHED)
                .like(StringUtils.hasText(query.getContent()), SystemAnnouncement::getContent, query.getContent())
                .orderByDesc(SystemAnnouncement::getPinned)
                .orderByDesc(SystemAnnouncement::getSortOrder)
                .orderByDesc(SystemAnnouncement::getPublishTime)
                .orderByDesc(SystemAnnouncement::getId);

        Page<SystemAnnouncement> page = page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        Map<Long, SystemAnnouncementUserState> stateMap = getStateMap(
                page.getRecords().stream().map(SystemAnnouncement::getId).toList(),
                currentUsername()
        );
        return toPageResult(page, stateMap, false);
    }

    @Override
    public int unreadCount() {
        String username = currentUsername();
        List<SystemAnnouncement> published = lambdaQuery()
                .eq(SystemAnnouncement::getStatus, STATUS_PUBLISHED)
                .list();
        if (published.isEmpty()) {
            return 0;
        }
        Map<Long, SystemAnnouncementUserState> stateMap = getStateMap(
                published.stream().map(SystemAnnouncement::getId).toList(),
                username
        );
        int count = 0;
        for (SystemAnnouncement announcement : published) {
            SystemAnnouncementUserState state = stateMap.get(announcement.getId());
            if (state == null || state.getReadTime() == null) {
                count++;
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        checkPublished(id);
        SystemAnnouncementUserState state = getOrCreateState(id, currentUsername());
        state.setReadTime(LocalDateTime.now());
        stateMapper.updateById(state);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPopupShown(Long id) {
        checkPublished(id);
        SystemAnnouncementUserState state = getOrCreateState(id, currentUsername());
        state.setPopupDate(LocalDate.now());
        stateMapper.updateById(state);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void silentToday(Long id) {
        checkPublished(id);
        SystemAnnouncementUserState state = getOrCreateState(id, currentUsername());
        state.setSilentDate(LocalDate.now());
        stateMapper.updateById(state);
    }

    private void fillByDto(SystemAnnouncement announcement, AnnouncementSaveDTO dto) {
        announcement.setContent(dto.getContent().trim());
        announcement.setPinned(dto.getPinned() == null ? 0 : dto.getPinned());
        announcement.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private SystemAnnouncement getExisting(Long id) {
        SystemAnnouncement announcement = getById(id);
        if (announcement == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return announcement;
    }

    private void checkPublished(Long id) {
        SystemAnnouncement announcement = getExisting(id);
        if (!Integer.valueOf(STATUS_PUBLISHED).equals(announcement.getStatus())) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "公告未发布");
        }
    }

    private SystemAnnouncementUserState getState(Long announcementId, String username) {
        return stateMapper.selectOne(new LambdaQueryWrapper<SystemAnnouncementUserState>()
                .eq(SystemAnnouncementUserState::getAnnouncementId, announcementId)
                .eq(SystemAnnouncementUserState::getUsername, username)
                .last("LIMIT 1"));
    }

    private SystemAnnouncementUserState getOrCreateState(Long announcementId, String username) {
        SystemAnnouncementUserState state = getState(announcementId, username);
        if (state != null) {
            return state;
        }
        state = new SystemAnnouncementUserState();
        state.setAnnouncementId(announcementId);
        state.setUsername(username);
        state.setIsDeleted(0);
        stateMapper.insert(state);
        return state;
    }

    private void resetUserStateForRepublish(Long announcementId) {
        stateMapper.update(null, new LambdaUpdateWrapper<SystemAnnouncementUserState>()
                .eq(SystemAnnouncementUserState::getAnnouncementId, announcementId)
                .set(SystemAnnouncementUserState::getReadTime, null)
                .set(SystemAnnouncementUserState::getPopupDate, null)
                .set(SystemAnnouncementUserState::getSilentDate, null));
    }

    private Map<Long, SystemAnnouncementUserState> getStateMap(List<Long> ids, String username) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return stateMapper.selectList(new LambdaQueryWrapper<SystemAnnouncementUserState>()
                        .in(SystemAnnouncementUserState::getAnnouncementId, ids)
                        .eq(SystemAnnouncementUserState::getUsername, username))
                .stream()
                .collect(Collectors.toMap(
                        SystemAnnouncementUserState::getAnnouncementId,
                        Function.identity(),
                        (first, ignored) -> first,
                        HashMap::new
                ));
    }

    private PageResult<AnnouncementVO> toPageResult(
            Page<SystemAnnouncement> page,
            Map<Long, SystemAnnouncementUserState> stateMap,
            boolean includePopup
    ) {
        Page<AnnouncementVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(item -> toVO(item, stateMap.get(item.getId()), includePopup))
                .toList());
        return PageResult.of(voPage);
    }

    private AnnouncementVO toVO(SystemAnnouncement announcement, SystemAnnouncementUserState state, boolean includePopup) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(announcement, vo);
        vo.setStatusDesc(statusDesc(announcement.getStatus()));
        if (state != null) {
            vo.setReadTime(state.getReadTime());
            vo.setPopupDate(state.getPopupDate());
            vo.setSilentDate(state.getSilentDate());
        }
        vo.setRead(vo.getReadTime() != null);
        if (includePopup) {
            LocalDate today = LocalDate.now();
            boolean silentToday = today.equals(vo.getSilentDate());
            vo.setShouldPopup(!vo.getRead() && !silentToday);
        } else {
            vo.setShouldPopup(false);
        }
        return vo;
    }

    private String statusDesc(Integer status) {
        if (Integer.valueOf(STATUS_PUBLISHED).equals(status)) {
            return "已发布";
        }
        if (Integer.valueOf(STATUS_OFFLINE).equals(status)) {
            return "已下线";
        }
        return "草稿";
    }

    private String currentUsername() {
        return CurrentUserUtil.getUsernameOrDefault("system");
    }
}
