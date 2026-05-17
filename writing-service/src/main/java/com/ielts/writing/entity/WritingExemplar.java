package com.ielts.writing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "writing_exemplars")
public class WritingExemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false)
    private Integer taskType;

    private String category;

    @Column(name = "band_score")
    private BigDecimal bandScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "examiner_comment", nullable = false, columnDefinition = "TEXT")
    private String examinerComment;

    private String source;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getBandScore() { return bandScore; }
    public void setBandScore(BigDecimal bandScore) { this.bandScore = bandScore; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getExaminerComment() { return examinerComment; }
    public void setExaminerComment(String examinerComment) { this.examinerComment = examinerComment; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
