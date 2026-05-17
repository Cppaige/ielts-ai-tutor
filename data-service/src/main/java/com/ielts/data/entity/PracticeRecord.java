package com.ielts.data.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_records")
public class PracticeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PracticeType type;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "service_record_id", nullable = false)
    private Long serviceRecordId;

    @Column(name = "overall_band")
    private BigDecimal overallBand;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PracticeType { WRITING, SPEAKING }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public PracticeType getType() { return type; }
    public void setType(PracticeType type) { this.type = type; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getServiceRecordId() { return serviceRecordId; }
    public void setServiceRecordId(Long serviceRecordId) { this.serviceRecordId = serviceRecordId; }
    public BigDecimal getOverallBand() { return overallBand; }
    public void setOverallBand(BigDecimal overallBand) { this.overallBand = overallBand; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
