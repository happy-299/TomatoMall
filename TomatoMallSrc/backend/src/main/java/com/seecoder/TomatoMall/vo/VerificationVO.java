package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.enums.VerificationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VerificationVO {
    private Integer id;

    /** 申请人基本信息 */
    private Integer accountId;
    private String username;          // 方便前端直接显示昵称 / 用户名

    /** 申请内容 */
    private String reasonText;
    private List<String> proofImgs;

    /** 状态 */
    private VerificationStatus status;
    private String rejectReason;

    /** 时间轴 */
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
