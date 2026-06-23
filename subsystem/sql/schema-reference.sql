create table if not exists tb_node (
    application_id bigint not null,
    node_id bigint primary key,
    analysis_mode varchar(1) not null default 'A',
    node_type varchar(30) not null,
    node_name varchar(2000) not null,
    hash varchar(300),
    use_yn varchar(1) not null default 'Y',
    create_user_id varchar(100) not null default 'system',
    create_date timestamp not null default now(),
    update_user_id varchar(100),
    update_date timestamp
);

create table if not exists tb_node_detail (
    node_id bigint not null,
    split_node_level int not null,
    split_node_name varchar(2000) not null,
    split_node_type varchar(30) not null,
    create_user_id varchar(100) not null default 'system',
    create_date timestamp not null default now(),
    update_user_id varchar(100),
    update_date timestamp,
    primary key (node_id, split_node_level)
);

create table if not exists tb_node_relation (
    application_id bigint not null,
    relation_id bigint primary key,
    node_id bigint not null,
    parent_node_id bigint not null,
    use_yn varchar(1) not null default 'Y'
);
