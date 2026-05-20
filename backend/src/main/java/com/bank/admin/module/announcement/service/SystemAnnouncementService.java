package com.bank.admin.module.announcement.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.announcement.dto.AnnouncementQueryDTO;
import com.bank.admin.module.announcement.dto.AnnouncementSaveDTO;
import com.bank.admin.module.announcement.entity.SystemAnnouncement;
import com.bank.admin.module.announcement.vo.AnnouncementVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SystemAnnouncementService extends IService<SystemAnnouncement> {

    PageResult<AnnouncementVO> adminPage(AnnouncementQueryDTO query);

    Long create(AnnouncementSaveDTO dto);

    void update(Long id, AnnouncementSaveDTO dto);

    void publish(Long id);

    void offline(Long id);

    void delete(Long id);

    AnnouncementVO latest();

    PageResult<AnnouncementVO> history(AnnouncementQueryDTO query);

    int unreadCount();

    void markRead(Long id);

    void markPopupShown(Long id);

    void silentToday(Long id);
}
