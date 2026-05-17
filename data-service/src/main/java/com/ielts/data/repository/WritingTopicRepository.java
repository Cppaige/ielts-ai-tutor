package com.ielts.data.repository;

import com.ielts.data.entity.WritingTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingTopicRepository extends JpaRepository<WritingTopic, Long> {
    List<WritingTopic> findByTaskType(Integer taskType);
    List<WritingTopic> findByCategory(String category);
}
