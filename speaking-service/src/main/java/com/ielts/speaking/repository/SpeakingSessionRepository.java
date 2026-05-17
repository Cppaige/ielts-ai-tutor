package com.ielts.speaking.repository;

import com.ielts.speaking.entity.SpeakingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {}
