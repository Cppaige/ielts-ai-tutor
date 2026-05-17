package com.ielts.speaking.repository;

import com.ielts.speaking.entity.SessionTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionTurnRepository extends JpaRepository<SessionTurn, Long> {
    List<SessionTurn> findBySessionIdOrderByTurnOrder(Long sessionId);
}
