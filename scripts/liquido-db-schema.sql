create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, checksum varchar(255), level integer not null, poll_id bigint not null, hashed_voter_token varchar(255) not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, law_model_order integer not null, primary key (ballot_model_id, law_model_order))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, proposal_id bigint not null, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_hashed_voter_token varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, nonce varchar(255) not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, duel_matrix varchar(255), status integer, title varchar(255) not null, voting_end_at timestamp, voting_start_at timestamp, area_id bigint not null, team_id bigint, winner_id bigint, primary key (id))
create table right_to_vote (hashed_voter_token varchar(255) not null, expires_at timestamp, area_id bigint, delegated_to_hashed_voter_token varchar(255), public_proxy_id bigint, primary key (hashed_voter_token))
create table teams (id bigint not null, created_at timestamp not null, updated_at timestamp not null, invite_code varchar(255), team_name varchar(255) not null, primary key (id))
create table teams_admins (team_model_id bigint not null, admins_id bigint not null, primary key (team_model_id, admins_id))
create table teams_members (team_model_id bigint not null, members_id bigint not null, primary key (team_model_id, members_id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, authy_id bigint not null, email varchar(255) not null, last_login timestamp, last_team_id bigint not null, mobilephone varchar(255), name varchar(255) not null, picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKqsjuulamh27u4sl74lp4v54d2 unique (poll_id, hashed_voter_token)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table polls add constraint UKj8cl20oebalvgywxu4mktf8l5 unique (title, team_id)
alter table right_to_vote add constraint UKbm9swuv2tl574p4gh3yuhx41w unique (area_id, public_proxy_id)
alter table teams add constraint UK_dsqu2wx93en6lbl2bnrjy7kol unique (team_name)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots add constraint FKlgjbvcvotbgjqnpxw5rns4jjn foreign key (hashed_voter_token) references right_to_vote
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments add constraint FK7puwbwe4yl8b2o32r798uw8q7 foreign key (proposal_id) references laws
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FKgbyhirqcwsqlmh9ehs8r6y8q1 foreign key (requested_delegation_from_hashed_voter_token) references right_to_vote
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FK6dt31yyguwl253bospr7ggplb foreign key (area_id) references areas
alter table polls add constraint FKrjatypfbyt1diivtbycf7skbg foreign key (team_id) references teams
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
alter table right_to_vote add constraint FKfh4tmxpy725saxy9m562r5hlc foreign key (area_id) references areas
alter table right_to_vote add constraint FK44pjh5srgaa08f3r1pf16byqq foreign key (delegated_to_hashed_voter_token) references right_to_vote
alter table right_to_vote add constraint FK8d3fb8gyi0koymfjy79cf20u3 foreign key (public_proxy_id) references users
alter table teams_admins add constraint FKlam5a42i278waum42496a0vb foreign key (admins_id) references users
alter table teams_admins add constraint FKex3bu9tfhwlsu7wq6ti2rvih3 foreign key (team_model_id) references teams
alter table teams_members add constraint FKgkdwlbkpn9kldrdkxw2on0ii4 foreign key (members_id) references users
alter table teams_members add constraint FKlyc4xdau3mwnx3vptaxv7k955 foreign key (team_model_id) references teams
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, checksum varchar(255), level integer not null, poll_id bigint not null, hashed_voter_token varchar(255) not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, law_model_order integer not null, primary key (ballot_model_id, law_model_order))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, proposal_id bigint not null, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_hashed_voter_token varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, nonce varchar(255) not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, duel_matrix varchar(255), status integer, title varchar(255) not null, voting_end_at timestamp, voting_start_at timestamp, area_id bigint not null, team_id bigint, winner_id bigint, primary key (id))
create table right_to_vote (hashed_voter_token varchar(255) not null, expires_at timestamp, area_id bigint, delegated_to_hashed_voter_token varchar(255), public_proxy_id bigint, primary key (hashed_voter_token))
create table teams (id bigint not null, created_at timestamp not null, updated_at timestamp not null, invite_code varchar(255), team_name varchar(255) not null, primary key (id))
create table teams_admins (team_model_id bigint not null, admins_id bigint not null, primary key (team_model_id, admins_id))
create table teams_members (team_model_id bigint not null, members_id bigint not null, primary key (team_model_id, members_id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, authy_id bigint not null, email varchar(255) not null, last_login timestamp, last_team_id bigint not null, mobilephone varchar(255), name varchar(255) not null, picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKqsjuulamh27u4sl74lp4v54d2 unique (poll_id, hashed_voter_token)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table polls add constraint UKj8cl20oebalvgywxu4mktf8l5 unique (title, team_id)
alter table right_to_vote add constraint UKbm9swuv2tl574p4gh3yuhx41w unique (area_id, public_proxy_id)
alter table teams add constraint UK_dsqu2wx93en6lbl2bnrjy7kol unique (team_name)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots add constraint FKlgjbvcvotbgjqnpxw5rns4jjn foreign key (hashed_voter_token) references right_to_vote
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments add constraint FK7puwbwe4yl8b2o32r798uw8q7 foreign key (proposal_id) references laws
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FKgbyhirqcwsqlmh9ehs8r6y8q1 foreign key (requested_delegation_from_hashed_voter_token) references right_to_vote
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FK6dt31yyguwl253bospr7ggplb foreign key (area_id) references areas
alter table polls add constraint FKrjatypfbyt1diivtbycf7skbg foreign key (team_id) references teams
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
alter table right_to_vote add constraint FKfh4tmxpy725saxy9m562r5hlc foreign key (area_id) references areas
alter table right_to_vote add constraint FK44pjh5srgaa08f3r1pf16byqq foreign key (delegated_to_hashed_voter_token) references right_to_vote
alter table right_to_vote add constraint FK8d3fb8gyi0koymfjy79cf20u3 foreign key (public_proxy_id) references users
alter table teams_admins add constraint FKlam5a42i278waum42496a0vb foreign key (admins_id) references users
alter table teams_admins add constraint FKex3bu9tfhwlsu7wq6ti2rvih3 foreign key (team_model_id) references teams
alter table teams_members add constraint FKgkdwlbkpn9kldrdkxw2on0ii4 foreign key (members_id) references users
alter table teams_members add constraint FKlyc4xdau3mwnx3vptaxv7k955 foreign key (team_model_id) references teams
