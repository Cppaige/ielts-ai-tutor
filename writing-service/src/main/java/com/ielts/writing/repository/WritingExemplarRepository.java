package com.ielts.writing.repository;

import com.ielts.writing.entity.WritingExemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingExemplarRepository extends JpaRepository<WritingExemplar, Long> {
    List<WritingExemplar> findByIdIn(List<Long> ids);
}
