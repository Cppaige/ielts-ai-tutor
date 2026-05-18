-- Create databases
CREATE DATABASE IF NOT EXISTS ielts_data CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ielts_writing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ielts_speaking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ielts_data tables
USE ielts_data;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    target_band DECIMAL(2,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE writing_topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type TINYINT NOT NULL,
    title TEXT NOT NULL,
    chart_type VARCHAR(50),
    chart_description TEXT,
    category VARCHAR(100),
    difficulty TINYINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE speaking_topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    part TINYINT NOT NULL,
    question TEXT NOT NULL,
    cue_card TEXT,
    follow_up_questions JSON,
    category VARCHAR(100),
    season VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE practice_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('WRITING','SPEAKING') NOT NULL,
    topic_id BIGINT NOT NULL,
    service_record_id BIGINT NOT NULL,
    overall_band DECIMAL(2,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_type (user_id, type, created_at DESC)
);

-- ielts_writing tables
USE ielts_writing;

CREATE TABLE writing_exemplars (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type TINYINT NOT NULL,
    category VARCHAR(100),
    band_score DECIMAL(2,1),
    excerpt TEXT NOT NULL,
    examiner_comment TEXT NOT NULL,
    source VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE writing_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    task_type TINYINT NOT NULL,
    essay_text TEXT NOT NULL,
    chart_type VARCHAR(50),
    chart_description TEXT,
    status ENUM('PENDING','SCORING','COMPLETED','FAILED') DEFAULT 'PENDING',
    tr_score DECIMAL(2,1),
    cc_score DECIMAL(2,1),
    lr_score DECIMAL(2,1),
    gra_score DECIMAL(2,1),
    overall_band DECIMAL(2,1),
    lr_gra_detail JSON,
    tr_cc_detail JSON,
    master_feedback JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scored_at TIMESTAMP NULL,
    INDEX idx_user (user_id, created_at DESC)
);

-- ielts_speaking tables
USE ielts_speaking;

CREATE TABLE speaking_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    examiner_persona ENUM('ENCOURAGING','STRICT') DEFAULT 'ENCOURAGING',
    status ENUM('IN_PROGRESS','COMPLETED','ABANDONED') DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    INDEX idx_user (user_id, started_at DESC)
);

CREATE TABLE session_turns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    part TINYINT NOT NULL,
    turn_order INT NOT NULL,
    role ENUM('EXAMINER','CANDIDATE') NOT NULL,
    content TEXT NOT NULL,
    audio_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id, turn_order)
);

CREATE TABLE speaking_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT UNIQUE NOT NULL,
    fluency_score DECIMAL(2,1),
    lexical_score DECIMAL(2,1),
    grammar_score DECIMAL(2,1),
    pronunciation_score DECIMAL(2,1),
    overall_band DECIMAL(2,1),
    detail JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create ielts user and grant privileges
CREATE USER IF NOT EXISTS 'ielts'@'%' IDENTIFIED BY 'ielts123';
GRANT ALL PRIVILEGES ON ielts_data.* TO 'ielts'@'%';
GRANT ALL PRIVILEGES ON ielts_writing.* TO 'ielts'@'%';
GRANT ALL PRIVILEGES ON ielts_speaking.* TO 'ielts'@'%';
FLUSH PRIVILEGES;
