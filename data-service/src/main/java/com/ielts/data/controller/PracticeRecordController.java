package com.ielts.data.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.data.entity.PracticeRecord;
import com.ielts.data.service.PracticeRecordService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data/practice-records")
public class PracticeRecordController {

    private final PracticeRecordService service;

    public PracticeRecordController(PracticeRecordService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<PracticeRecord>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (type != null) {
            return ApiResponse.success(service.listByUserAndType(userId, PracticeRecord.PracticeType.valueOf(type), page, size));
        }
        return ApiResponse.success(service.listByUser(userId, page, size));
    }
}
