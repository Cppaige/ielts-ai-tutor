package com.ielts.writing.repository;

import com.ielts.writing.entity.WritingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {
    List<WritingSubmission> findByUserIdOrderByCreatedAtDesc(Long userId);
}
