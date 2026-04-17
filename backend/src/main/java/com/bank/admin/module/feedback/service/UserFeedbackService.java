package com.bank.admin.module.feedback.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.feedback.dto.FeedbackCreateDTO;
import com.bank.admin.module.feedback.dto.FeedbackQueryDTO;
import com.bank.admin.module.feedback.dto.FeedbackRemarkDTO;
import com.bank.admin.module.feedback.dto.FeedbackStatusUpdateDTO;
import com.bank.admin.module.feedback.dto.FeedbackUpdateDTO;
import com.bank.admin.module.feedback.vo.FeedbackStatsVO;
import com.bank.admin.module.feedback.vo.FeedbackVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户反馈 Service
 */
public interface UserFeedbackService {

    /** 分页查询 */
    PageResult<FeedbackVO> page(FeedbackQueryDTO query);

    /** 详情（含附件 + 处理轨迹） */
    FeedbackVO detail(Long id);

    /** 提交反馈（含附件上传） */
    Long create(FeedbackCreateDTO dto, List<MultipartFile> files);

    /** 编辑反馈 */
    void update(Long id, FeedbackUpdateDTO dto, List<MultipartFile> files);

    /** 修改状态 */
    void updateStatus(Long id, FeedbackStatusUpdateDTO dto);

    /** 追加处理备注 */
    void addRemark(Long id, FeedbackRemarkDTO dto);

    /** 统计数据 */
    FeedbackStatsVO stats();

    /** 下载附件（返回文件路径） */
    String getAttachmentFilePath(Long attachmentId);

    /** 删除附件 */
    void deleteAttachment(Long attachmentId);

    /** 逻辑删除反馈 */
    void delete(Long id);
}
