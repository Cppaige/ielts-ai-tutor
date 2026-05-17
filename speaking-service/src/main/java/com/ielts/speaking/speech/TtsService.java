package com.ielts.speaking.speech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TtsService {

    private final String appKey;

    public TtsService(@Value("${aliyun.nls.app-key}") String appKey) {
        this.appKey = appKey;
    }

    public String synthesize(String text) {
        // MVP placeholder: integrate with Aliyun NLS SDK
        // Real implementation will use SpeechSynthesizer, save to file, return URL
        throw new UnsupportedOperationException("TTS integration pending - use mock in tests");
    }
}
