create table memos
(
    id         bigserial
        primary key,
    name       varchar(255),
    content    text,
    created_at timestamp,
    updated_at timestamp
);
