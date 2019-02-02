create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws
create sequence hibernate_sequence start with 1 increment by 1
create table areas (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(255) not null, title varchar(255) not null, created_by_id bigint not null, primary key (id))
create table ballots (id bigint not null, level integer not null, vote_count bigint not null, checksum varchar(255) not null, poll_id bigint not null, primary key (id))
create table ballots_vote_order (ballot_model_id bigint not null, vote_order_id bigint not null, vote_order_order integer not null, primary key (ballot_model_id, vote_order_order))
create table checksums (checksum varchar(255) not null, expires_at timestamp, transitive boolean not null, area_id bigint, delegated_to_checksum varchar(255), public_proxy_id bigint, primary key (checksum))
create table comments (id bigint not null, created_at timestamp not null, updated_at timestamp not null, comment varchar(255) not null, created_by_id bigint not null, law_id bigint not null, parent_id bigint, primary key (id))
create table comments_down_voters (comment_model_id bigint not null, down_voters_id bigint not null, primary key (comment_model_id, down_voters_id))
create table comments_up_voters (comment_model_id bigint not null, up_voters_id bigint not null, primary key (comment_model_id, up_voters_id))
create table delegations (id bigint not null, created_at timestamp not null, updated_at timestamp not null, requested_delegation_at timestamp, transitive boolean not null, area_id bigint not null, from_user_id bigint not null, requested_delegation_from_checksum_checksum varchar(255), to_proxy_id bigint not null, primary key (id))
create table key_value (id bigint not null, key_col varchar(255) not null, value_col varchar(255), primary key (id))
create table laws (id bigint not null, created_at timestamp not null, updated_at timestamp not null, description varchar(1000) not null, reached_quorum_at timestamp, status integer not null, title varchar(255) not null, area_id bigint not null, created_by_id bigint, poll_id bigint, primary key (id))
create table laws_comments (law_model_id bigint not null, comments_id bigint not null, primary key (law_model_id, comments_id))
create table laws_supporters (law_model_id bigint not null, supporters_id bigint not null, primary key (law_model_id, supporters_id))
create table one_time_token (id bigint not null, created_at timestamp not null, updated_at timestamp not null, token varchar(255) not null, token_type integer not null, valid_until timestamp not null, user_id bigint not null, primary key (id))
create table polls (id bigint not null, created_at timestamp not null, updated_at timestamp not null, status integer, voting_end_at timestamp, voting_start_at timestamp, winner_id bigint, primary key (id))
create table users (id bigint not null, created_at timestamp not null, updated_at timestamp not null, email varchar(255) not null, last_login timestamp, mobilephone varchar(255), name varchar(255), picture varchar(255), website varchar(255), primary key (id))
alter table areas add constraint UK_p61hnj1m450knduscxoldchfj unique (title)
alter table ballots add constraint UKhwwepk8n3ce54lerewx2fmyxf unique (poll_id, checksum)
alter table checksums add constraint UKl0xnklnhlk4ptwsu9b0qxtybn unique (area_id, public_proxy_id)
alter table delegations add constraint UK2wyh7poif8npdwlur8dford0j unique (area_id, from_user_id)
alter table key_value add constraint UK_8te0dhiexeowa1rgva302to1l unique (key_col)
alter table laws add constraint UK_bs516opv3hontptwvpioos72b unique (title)
alter table laws_comments add constraint UK_of3yv6x3mahjphjbje5xhrxc8 unique (comments_id)
alter table users add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email)
alter table users add constraint UK_70qe9wofm8uj2peanwkpherrm unique (mobilephone)
alter table areas add constraint FKp9n0u4hm2q1o7v0068c8gd4el foreign key (created_by_id) references users
alter table ballots add constraint FK1rrv2wy16uqonfl9cr1cqg2xd foreign key (checksum) references checksums
alter table ballots add constraint FKog7shplj1ccoh8n9ao4y16lq3 foreign key (poll_id) references polls
alter table ballots_vote_order add constraint FKowd5mad84qvwgst43698a6gx5 foreign key (vote_order_id) references laws
alter table ballots_vote_order add constraint FKih6thewvjeh3ow9dl1ytj9awb foreign key (ballot_model_id) references ballots
alter table checksums add constraint FKllw57xbw0d67optmwfmu3p2pc foreign key (area_id) references areas
alter table checksums add constraint FKdflpse9fsogy1fmfdy2w79rg foreign key (delegated_to_checksum) references checksums
alter table checksums add constraint FK68ftlvl0mwtvoqd3bfupp0xrg foreign key (public_proxy_id) references users
alter table comments add constraint FKakkm6qfydu7vgnfne1yo0xmed foreign key (created_by_id) references users
alter table comments add constraint FK4v1hi5ntv7k7xl8sf7kr91ana foreign key (law_id) references laws
alter table comments add constraint FKlri30okf66phtcgbe5pok7cc0 foreign key (parent_id) references comments
alter table comments_down_voters add constraint FK62ilac17tg4cpkrheonud72hv foreign key (down_voters_id) references users
alter table comments_down_voters add constraint FKjymyj54lenhs4ke6myqhly72 foreign key (comment_model_id) references comments
alter table comments_up_voters add constraint FKeuurm027pdc4l5a9g9o2l0uyd foreign key (up_voters_id) references users
alter table comments_up_voters add constraint FK8ls8t571y99vttw1mdkebxagp foreign key (comment_model_id) references comments
alter table delegations add constraint FKswbdeovc7hsq5ex30pbmdu9d4 foreign key (area_id) references areas
alter table delegations add constraint FK9koya2mi6t18vf7u1frobkniq foreign key (from_user_id) references users
alter table delegations add constraint FK1y1r0d478r9ntdmuiykkuhadv foreign key (requested_delegation_from_checksum_checksum) references checksums
alter table delegations add constraint FK21a6lg8fu2js6e0ju41wj2gqp foreign key (to_proxy_id) references users
alter table laws add constraint FK9viqdj5ldun0ahwqdff1wuy39 foreign key (area_id) references areas
alter table laws add constraint FKtr1pll46q50shwbffyiyuavgn foreign key (created_by_id) references users
alter table laws add constraint FK4quhd81mm84x6rq0ccbxseyxx foreign key (poll_id) references polls
alter table laws_comments add constraint FKfl0y4qfvfpf9ynmmfj0jevxff foreign key (comments_id) references comments
alter table laws_comments add constraint FK36fbkiuitrv0yrvy8s0ig46ix foreign key (law_model_id) references laws
alter table laws_supporters add constraint FKk8mvjc4rq970ojavyfia9bqve foreign key (supporters_id) references users
alter table laws_supporters add constraint FK9b4i87yikuchrirfjaymgohe3 foreign key (law_model_id) references laws
alter table one_time_token add constraint FK4o21xt1rmricb3ea4to5nkr8d foreign key (user_id) references users
alter table polls add constraint FKqadovsmq4uok58sp9hkhwjqis foreign key (winner_id) references laws