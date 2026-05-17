package com.ielts.data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_topics")
public class SpeakingTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer part;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "cue_card", columnDefinition = "TEXT")
    private String cueCard;

    @Column(name = "follow_up_questions", columnDefinition = "JSON")
    private String followUpQuestions;

    private String category;

    private String season;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPart() { return part; }
    public void setPart(Integer part) { this.part = part; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getCueCard() { return cueCard; }
    public void setCueCard(String cueCard) { this.cueCard = cueCard; }
    public String getFollowUpQuestions() { return followUpQuestions; }
    public void setFollowUpQuestions(String followUpQuestions) { this.followUpQuestions = followUpQuestions; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
