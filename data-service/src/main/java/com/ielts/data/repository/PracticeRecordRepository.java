package com.ielts.data.repository;

import com.ielts.data.entity.PracticeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeRecordRepository extends JpaRepository<PracticeRecord, Long> {
    Page<PracticeRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<PracticeRecord> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, PracticeRecord.PracticeType type, Pageable pageable);
}
