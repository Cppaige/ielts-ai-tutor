package com.ielts.speaking.speech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AsrService {

    private final String appKey;

    public AsrService(@Value("${aliyun.nls.app-key}") String appKey) {
        this.appKey = appKey;
    }

    public String transcribe(byte[] audioData) {
        // MVP placeholder: returns empty string until Aliyun NLS SDK is integrated
        return "";
    }
}
