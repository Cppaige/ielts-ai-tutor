package com.ielts.speaking.statemachine;

public enum SessionState {
    PART1_QA,
    PART2_INTRO,
    PART2_CANDIDATE_SPEAKING,
    PART3_DISCUSSION,
    SESSION_ENDED;

    public SessionState next() {
        return switch (this) {
            case PART1_QA -> PART2_INTRO;
            case PART2_INTRO -> PART2_CANDIDATE_SPEAKING;
            case PART2_CANDIDATE_SPEAKING -> PART3_DISCUSSION;
            case PART3_DISCUSSION -> SESSION_ENDED;
            case SESSION_ENDED -> SESSION_ENDED;
        };
    }
}
