package com.ielts.data.service;

import com.ielts.data.dto.PracticeRecordMessage;
import com.ielts.data.entity.PracticeRecord;
import com.ielts.data.repository.PracticeRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PracticeRecordService {

    private final PracticeRecordRepository repository;

    public PracticeRecordService(PracticeRecordRepository repository) {
        this.repository = repository;
    }

    public void saveFromMessage(PracticeRecordMessage message) {
        PracticeRecord record = new PracticeRecord();
        record.setUserId(message.userId());
        record.setTopicId(message.topicId());
        record.setServiceRecordId(message.serviceRecordId());
        record.setType(PracticeRecord.PracticeType.valueOf(message.type()));
        record.setOverallBand(message.overallBand());
        repository.save(record);
    }

    public Page<PracticeRecord> listByUser(Long userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public Page<PracticeRecord> listByUserAndType(Long userId, PracticeRecord.PracticeType type, int page, int size) {
        return repository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, PageRequest.of(page, size));
    }
}
