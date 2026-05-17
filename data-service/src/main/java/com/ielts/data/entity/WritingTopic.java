package com.ielts.data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "writing_topics")
public class WritingTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false)
    private Integer taskType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "chart_type")
    private String chartType;

    @Column(name = "chart_description", columnDefinition = "TEXT")
    private String chartDescription;

    private String category;

    private Integer difficulty;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getChartDescription() { return chartDescription; }
    public void setChartDescription(String chartDescription) { this.chartDescription = chartDescription; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
