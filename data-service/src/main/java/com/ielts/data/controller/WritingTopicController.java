package com.ielts.data.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.data.entity.WritingTopic;
import com.ielts.data.service.WritingTopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data/writing-topics")
public class WritingTopicController {

    private final WritingTopicService service;

    public WritingTopicController(WritingTopicService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<WritingTopic>> list(@RequestParam(required = false) Integer taskType) {
        if (taskType != null) {
            return ApiResponse.success(service.listByTaskType(taskType));
        }
        return ApiResponse.success(service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<WritingTopic> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
