package com.ielts.data.repository;

import com.ielts.data.entity.SpeakingTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpeakingTopicRepository extends JpaRepository<SpeakingTopic, Long> {
    List<SpeakingTopic> findByPart(Integer part);
    List<SpeakingTopic> findByCategory(String category);
    List<SpeakingTopic> findByPartAndCategory(Integer part, String category);
}
