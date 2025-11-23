-- V2: Companion Schema
-- AI Companion tables for NexusAI platform

-- Companions table
CREATE TABLE companions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(150),
    description TEXT,
    avatar_url VARCHAR(500),
    voice_id VARCHAR(100),
    personality_type VARCHAR(50) NOT NULL DEFAULT 'friendly',
    base_prompt TEXT,
    system_instructions TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    model_provider VARCHAR(50) DEFAULT 'ollama',
    model_name VARCHAR(100) DEFAULT 'llama3.2',
    temperature DECIMAL(3,2) DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2048,
    total_conversations INTEGER DEFAULT 0,
    total_messages INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Companion traits (personality characteristics)
CREATE TABLE companion_traits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    companion_id UUID NOT NULL REFERENCES companions(id) ON DELETE CASCADE,
    trait_name VARCHAR(100) NOT NULL,
    trait_value VARCHAR(255),
    weight DECIMAL(3,2) DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Companion knowledge base
CREATE TABLE companion_knowledge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    companion_id UUID NOT NULL REFERENCES companions(id) ON DELETE CASCADE,
    knowledge_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    source_url VARCHAR(500),
    embedding VECTOR(1536),
    metadata JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Companion categories
CREATE TABLE companion_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(50),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Companion to category mapping
CREATE TABLE companion_category_mapping (
    companion_id UUID NOT NULL REFERENCES companions(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES companion_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (companion_id, category_id)
);

-- Companion ratings
CREATE TABLE companion_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    companion_id UUID NOT NULL REFERENCES companions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(companion_id, user_id)
);

-- Companion favorites
CREATE TABLE companion_favorites (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    companion_id UUID NOT NULL REFERENCES companions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, companion_id)
);

-- Indexes
CREATE INDEX idx_companions_user_id ON companions(user_id);
CREATE INDEX idx_companions_is_public ON companions(is_public) WHERE is_public = TRUE;
CREATE INDEX idx_companions_is_active ON companions(is_active);
CREATE INDEX idx_companions_personality ON companions(personality_type);
CREATE INDEX idx_companion_traits_companion ON companion_traits(companion_id);
CREATE INDEX idx_companion_knowledge_companion ON companion_knowledge(companion_id);
CREATE INDEX idx_companion_knowledge_type ON companion_knowledge(knowledge_type);
CREATE INDEX idx_companion_ratings_companion ON companion_ratings(companion_id);
CREATE INDEX idx_companion_favorites_user ON companion_favorites(user_id);

-- Full text search index
CREATE INDEX idx_companions_search ON companions USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
