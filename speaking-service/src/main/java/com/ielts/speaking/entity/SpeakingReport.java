package com.ielts.speaking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_reports")
public class SpeakingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false)
    private Long sessionId;

    @Column(name = "fluency_score")
    private BigDecimal fluencyScore;

    @Column(name = "lexical_score")
    private BigDecimal lexicalScore;

    @Column(name = "grammar_score")
    private BigDecimal grammarScore;

    @Column(name = "pronunciation_score")
    private BigDecimal pronunciationScore;

    @Column(name = "overall_band")
    private BigDecimal overallBand;

    @Column(columnDefinition = "JSON")
    private String detail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public BigDecimal getFluencyScore() { return fluencyScore; }
    public void setFluencyScore(BigDecimal fluencyScore) { this.fluencyScore = fluencyScore; }
    public BigDecimal getLexicalScore() { return lexicalScore; }
    public void setLexicalScore(BigDecimal lexicalScore) { this.lexicalScore = lexicalScore; }
    public BigDecimal getGrammarScore() { return grammarScore; }
    public void setGrammarScore(BigDecimal grammarScore) { this.grammarScore = grammarScore; }
    public BigDecimal getPronunciationScore() { return pronunciationScore; }
    public void setPronunciationScore(BigDecimal pronunciationScore) { this.pronunciationScore = pronunciationScore; }
    public BigDecimal getOverallBand() { return overallBand; }
    public void setOverallBand(BigDecimal overallBand) { this.overallBand = overallBand; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
