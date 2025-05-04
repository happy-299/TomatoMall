package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.enums.VerificationStatus;
import com.seecoder.TomatoMall.vo.VerificationVO;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Basic
    private String reasonText;

    @ElementCollection
    @CollectionTable(
            name = "verification_application_img",
            joinColumns = @JoinColumn(name = "application_id")
    )
    @OrderColumn(name = "idx")
    private List<String> proofImgs = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Account reviewer;

    private LocalDateTime reviewTime;

    private LocalDateTime createTime = LocalDateTime.now();

    private String rejectReason;

    public VerificationVO toVO() {
        VerificationVO vo = new VerificationVO();
        vo.setId(this.id);
        vo.setAccountId(this.account.getId());
        vo.setUsername(this.account.getUsername());
        vo.setReasonText(this.reasonText);
        vo.setProofImgs(this.proofImgs);
        vo.setStatus(this.status);
        vo.setRejectReason(this.rejectReason);
        vo.setCreateTime(this.createTime);
        vo.setReviewTime(this.reviewTime);
        return vo;
    }
}
