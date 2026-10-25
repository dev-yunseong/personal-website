CREATE TABLE persistent_logins (
    series VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_persistent_logins_username ON persistent_logins(username);

CREATE TABLE project_reports (
    slug VARCHAR(64) PRIMARY KEY,
    payload TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
