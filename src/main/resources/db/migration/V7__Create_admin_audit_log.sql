CREATE TABLE admin_audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    previous_value VARCHAR(255),
    new_value VARCHAR(255),
    reason VARCHAR(32),
    details VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_created_at ON admin_audit_log (created_at DESC);
CREATE INDEX idx_audit_entity ON admin_audit_log (entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_actor ON admin_audit_log (actor, created_at DESC);
