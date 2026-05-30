package com.bank.admin.module.book.dto;

import com.bank.admin.module.book.vo.WechatBillImportRowVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "WeChat bill import confirm payload")
public class WechatBillImportConfirmDTO {

    @NotEmpty(message = "导入明细不能为空")
    private List<WechatBillImportRowVO> rows;
}
