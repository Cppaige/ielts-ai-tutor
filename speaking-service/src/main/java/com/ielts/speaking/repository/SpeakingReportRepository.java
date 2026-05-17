package com.ielts.speaking.repository;

import com.ielts.speaking.entity.SpeakingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpeakingReportRepository extends JpaRepository<SpeakingReport, Long> {
    Optional<SpeakingReport> findBySessionId(Long sessionId);
}
