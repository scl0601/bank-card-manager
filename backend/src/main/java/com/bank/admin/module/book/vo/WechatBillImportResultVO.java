package com.bank.admin.module.book.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "WeChat bill import result")
public class WechatBillImportResultVO {

    private String batchNo;
    private Integer totalRows = 0;
    private Integer importableRows = 0;
    private Integer importedRows = 0;
    private Integer duplicateRows = 0;
    private Integer pendingRows = 0;
    private Integer errorRows = 0;
    private List<WechatBillImportRowVO> rows = new ArrayList<>();
}
