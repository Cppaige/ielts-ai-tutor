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
        // MVP placeholder: integrate with Aliyun NLS SDK
        // Real implementation will use SpeechRecognizer from nls-sdk-tts
        throw new UnsupportedOperationException("ASR integration pending - use mock in tests");
    }
}
