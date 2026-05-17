package com.ielts.writing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "writing_submissions")
public class WritingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "task_type", nullable = false)
    private Integer taskType;

    @Column(name = "essay_text", nullable = false, columnDefinition = "TEXT")
    private String essayText;

    @Column(name = "chart_type")
    private String chartType;

    @Column(name = "chart_description", columnDefinition = "TEXT")
    private String chartDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "tr_score")
    private BigDecimal trScore;

    @Column(name = "cc_score")
    private BigDecimal ccScore;

    @Column(name = "lr_score")
    private BigDecimal lrScore;

    @Column(name = "gra_score")
    private BigDecimal graScore;

    @Column(name = "overall_band")
    private BigDecimal overallBand;

    @Column(name = "lr_gra_detail", columnDefinition = "JSON")
    private String lrGraDetail;

    @Column(name = "tr_cc_detail", columnDefinition = "JSON")
    private String trCcDetail;

    @Column(name = "master_feedback", columnDefinition = "JSON")
    private String masterFeedback;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "scored_at")
    private LocalDateTime scoredAt;

    public enum SubmissionStatus { PENDING, SCORING, COMPLETED, FAILED }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }
    public String getEssayText() { return essayText; }
    public void setEssayText(String essayText) { this.essayText = essayText; }
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getChartDescription() { return chartDescription; }
    public void setChartDescription(String chartDescription) { this.chartDescription = chartDescription; }
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public BigDecimal getTrScore() { return trScore; }
    public void setTrScore(BigDecimal trScore) { this.trScore = trScore; }
    public BigDecimal getCcScore() { return ccScore; }
    public void setCcScore(BigDecimal ccScore) { this.ccScore = ccScore; }
    public BigDecimal getLrScore() { return lrScore; }
    public void setLrScore(BigDecimal lrScore) { this.lrScore = lrScore; }
    public BigDecimal getGraScore() { return graScore; }
    public void setGraScore(BigDecimal graScore) { this.graScore = graScore; }
    public BigDecimal getOverallBand() { return overallBand; }
    public void setOverallBand(BigDecimal overallBand) { this.overallBand = overallBand; }
    public String getLrGraDetail() { return lrGraDetail; }
    public void setLrGraDetail(String lrGraDetail) { this.lrGraDetail = lrGraDetail; }
    public String getTrCcDetail() { return trCcDetail; }
    public void setTrCcDetail(String trCcDetail) { this.trCcDetail = trCcDetail; }
    public String getMasterFeedback() { return masterFeedback; }
    public void setMasterFeedback(String masterFeedback) { this.masterFeedback = masterFeedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getScoredAt() { return scoredAt; }
    public void setScoredAt(LocalDateTime scoredAt) { this.scoredAt = scoredAt; }
}
