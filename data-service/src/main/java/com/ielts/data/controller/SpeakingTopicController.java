package com.ielts.data.controller;

import com.ielts.common.dto.ApiResponse;
import com.ielts.data.entity.SpeakingTopic;
import com.ielts.data.service.SpeakingTopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data/speaking-topics")
public class SpeakingTopicController {

    private final SpeakingTopicService service;

    public SpeakingTopicController(SpeakingTopicService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SpeakingTopic>> listByPart(
            @RequestParam Integer part,
            @RequestParam(required = false) String category) {
        if (category != null) {
            return ApiResponse.success(service.listByPartAndCategory(part, category));
        }
        return ApiResponse.success(service.listByPart(part));
    }

    @GetMapping("/{id}")
    public ApiResponse<SpeakingTopic> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
