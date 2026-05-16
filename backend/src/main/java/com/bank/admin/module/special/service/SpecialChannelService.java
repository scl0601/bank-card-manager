package com.bank.admin.module.special.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.special.dto.SpecialBillAfterYearDeleteDTO;
import com.bank.admin.module.special.dto.SpecialBillBatchDeleteDTO;
import com.bank.admin.module.special.dto.SpecialBillQueryDTO;
import com.bank.admin.module.special.dto.SpecialBillUpdateDTO;
import com.bank.admin.module.special.dto.SpecialCardSaveDTO;
import com.bank.admin.module.special.dto.SpecialProfitExtraFeeUpdateDTO;
import com.bank.admin.module.special.dto.SpecialProfitQueryDTO;
import com.bank.admin.module.special.dto.SpecialUserConfigSaveDTO;
import com.bank.admin.module.special.vo.SpecialBillVO;
import com.bank.admin.module.special.vo.SpecialBillImportResultVO;
import com.bank.admin.module.special.vo.SpecialCardVO;
import com.bank.admin.module.special.vo.SpecialConfigVO;
import com.bank.admin.module.special.vo.SpecialProfitStatsVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SpecialChannelService {

    SpecialConfigVO getConfig();

    SpecialConfigVO saveConfig(SpecialUserConfigSaveDTO dto);

    List<SpecialCardVO> listCards();

    Long saveCard(SpecialCardSaveDTO dto);

    void updateCard(SpecialCardSaveDTO dto);

    void deleteCard(Long id);

    PageResult<SpecialBillVO> pageBills(SpecialBillQueryDTO query);

    void updateBill(SpecialBillUpdateDTO dto);

    SpecialBillImportResultVO importBills(MultipartFile file, Integer year);

    int deleteBillsBeforeYear(SpecialBillBatchDeleteDTO dto);

    int deleteBillsAfterYear(SpecialBillAfterYearDeleteDTO dto);

    void batchDeleteBills(List<Long> ids);

    void updateProfitExtraFees(SpecialProfitExtraFeeUpdateDTO dto);

    SpecialProfitStatsVO stats(SpecialProfitQueryDTO query);
}
