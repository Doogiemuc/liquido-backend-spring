# LIQUIDO - Tech Guide

This is the technical documentation of the LQIUIDO backend. The backend is implemented in Java SPRING BOOT.

# Data Model

## User and Account Management

Every `user` is `member` of a `team`. A `user` may be a member in several `teams`. Even with the same email address.
Every `team` has one `admin`. The `admin` of a `team` is the only one who can create polls. 
A `Team` holds `polls`. Every `user` can add his `proposal` to a `poll`. Every user is only allowed to have 
exactly one `proposal` in each `poll`. Only the admin is allowed to add several `porposals` to a `poll` in his `team`.

The `admin` starts the `voting phase` of a `poll`. Then every `member` of the `team` can cast his vote in this poll.

//TODO: Should it be possible to allow other `users` to cast their vote? Only for specifically invited users? Or also for completely public polls?
//      Hey this is easy. Voting is anonymous anyway: Admin can simply send valid Right2Votes to anyone. Not just registered users.
//TODO: Need extra page to really cast a vote anonymously. No login required. Only a valid right2Vote token.


## Process Model from an idea to a Law

 - At first there is a short rough idea. (NEW_IDEA)
 - When an idea reaches its quorum, then it becomes a proposal. (NEW_PROPOSAL)
 - Alternative proposals can be added to that initial proposal. (NEW_PROPOSAL)
   But they first need to reach a certain quorum too, before they can be voted upon.
 - Each proposal can be discussed. Comments can be voted up and down. (ELABORATION)
 - n days after the initial proposal reached its quorum, then the voting phase starts.
   All competing proposals can be voted upon. (VOTING_PHASE)
 - The winning proposal becomes a law (LAW) and all other proposals are (DECLINED)

## Wording

"idea"          := rough and short
"proposal"      := reached its quorum
"law"           := won a vote

# Use Cases

## Login

##### Sequence Diagram for login flow

See Liquido Authy Login Sequence.plantuml   and .svg




## Backlog / User Stories

## Global configuration / settings

   - number of likes that are necessary to move an idea onto the table
   - number of supporters that are necessary for a proposal to reach its quorum

## Team
 - A Team has at least one admin and several members
 - Only the admin can create new polls in the team
 - Then everyone in the team can add proposals to that poll
 - 

TODO: configurations for each team
 - Who can create polls and proposals?
 - Do new team members that join via link need to be confirmed?
 - 


## "Profile" default values for new polls

  - when does a voting phase / poll start
    a) n days after the initial proposal reached its quorum
    b) when there are at least n alternative proposals (with quorum each)
    c) manually started by the admin
  - duration of elaboration phase
  - duration of voting phase

### Ideas

 - I "Like to discuss" an idea
 - Move an idea onto the table when the necessary quorum is reached.

### Laws

 - Edit the description of a law proposal
 - Add a suggestion to a law
 - Up/Down-vote a suggestion.
 - Sort competing proposals into vote order (XXL)

 ### Voter Token

 A voter token is the digital representation of your right to vote.
 You can request a voter token for every area.

 You need a voterToken to
  - cast your vote
  - check for an existing ballot in a given poll
  - assign, edit or remove a proxy
  - accept delegation requests
  - become a public proxy
  - get number of real delegations to your checksum

Get proxy info for an area is possible without a voterToken


 ### Token Checksum

 The token checksum is the hashed value of your voter token. And a server secret, so that only the
 server is able to create valid checksums.


     Voter =request=> Voter Token =hashed=> Voter's Checksum =delegatedTo=> Proxies Checksum <= ProxyToken <= Proxy


# User management 

At the beginning I had no idea how important and unexpectedly complex user management can be. Especially for an app like LIQUIDO very security is crucial.

## Authy

There are no passwords in LIQUIDO! Instead users can login with One Time Tokens. A user can either install the Authy App on his mobile phone
and then use the time based one time tokens (T-OTP) that the authy app gererates. Authy can also send OTPs via SMS or email.

## Register

When the user registers, he is told to install the Authy mobile app (by twilio.com).
The Authy app will automatically show LIQUIDO as an authy "account", when the user provices 
the same mobilephone.

## Login

At the login page the user can either login via e-mail or via authy. 

### Login via authy

User must enter his mobilephone, click request token and then enter the T-OTP from the authy app.

# REST API

//TODO: autogenerate Swagger API doc 

//TODO: https://github.com/slatedocs/slate

//TODO: Vue.js writes its documentation in https://hexo.io/   see https://github.com/vuejs/vuejs.org

https://github.com/vuejs/vuejs.org/blob/master/writing-guide.md

Greate Hexo Theme by Zalando!
https://zalando-incubator.github.io/hexo-theme-doc/

GET /my/user
GET /my/voterToken/{areaId}

### Assign, edit and remove a proxy

GET /my/proxy/{areaId}                      collect all info about the proxy and delegations in that area
PUT /my/proxy/{areaId}  AssignProxyRequest  Assign (or reassign) a proxy
DELETE /my/proxy/{areaId}                   Remove a proxy in that area

### Being and becoming a proxy onself

GET /my/delegations/{areaId}                       get accepted and requested delegations
PUT /my/delegations/{areaId}/accept?voterToken=... accept delegation requests
PUT /my/delegations/{areaId}/becomePublicProxy     become a public proxy in that area

GET /users/{userId}/publicChecksum         get checksum of public proxy



# Liquido CI build pipeline

## Compile and build

 * Rename `src/main/resources/application.properties.example` to `application.properties` and fill in all necessary passwords.
 * Build `mvn package`
 * Run: `mvn install && mvn run:jetty`

## Docker build

 * Check your database configuration in the default `src/java/resources/application.properties` file
 * If you are on Windows: Start "Docker for Windows" `mvn dockerfile:build`

## Run docker container

 * `docker run -p 8080:8080 org.doogie/liquido-backend-spring`
   will create and start a docker container and expose port 8080. Check that no other process is already running on that port!
 * Reconnect to logs of an already running container: `docker logs -f [CONTAINER_ID]`

## Docker commands on WIN

 * `docker ps -a -q | ForEach { docker stop $_ }`  - stop all running containers

## Build a release with maven

 * `mvn release:prepare` -> Enter name for tag (can accept default) and next development version
 *  This will also run all tests. There must not be any uncommitted local changes in your working directory.
 *  This will automatically increment the build number (`mvn buildnumber:create`)

# Database

    SELECT CONCAT('SHOW GRANTS FOR ''',user,'''@''',host,''';') FROM mysql.user;



# Dev Links

* There is an Swagger API documentation (autogenerated with Springfox) at  http://localhost:8080/swagger-ui.html
* [H2 DB Web Console](http://localhost:8080/h2-console)   JDBC URL: `jdbc:h2:mem:testdb`   Username: `sa` - no pwd, Driver Class: `org.h2.Driver`    * https://springframework.guru/using-the-h2-database-console-in-spring-boot-with-spring-security/



# Testing

We take testing very seriously in Liquido! There is an automated test suite for backend on unit- and integration level.

### Test data

Tests need data! In theory every test should create its own test data. All tests should be completely independent from each other.
But this is just theory. Once you have a lot of tests in a larger project your test suite simply becomes to slow.
So we need another solution: In Liquido there is a dedicated `TestDataCreator.java`. It relies on a set of fixed test data in
`TestFixtures.java`. When the tests run for the first time, then TestDataCreator create all necessary test data from scratch
and it exports the database schema and the testdata as a SQL script. In following test runs, that can quickly be imported
back into the database.  

To re-create all testdata run the backend with the following environment variables. BE CAREFULL: This will delete and kill your whole DB!!!

    -Dspring.profiles.active=dev -DrecreateTestData=true -Dspring.jpa.hibernate.ddl-auto=create

From then on all tests can be run against this pre-created test data. This is fast!

    -Dspring.profiles.active=dev -DloadTestData=true     -Dspring.jpa.hibernate-ddl-auto=none

## Run all tests

Run the test cases under `srs/test/java` with `mvn test -DloadSampleDB=true`.
Spring Boot will automatically start the test server for you.


### Testing against production environment

After a deployment you should run tests against your production environment. You may do this manually. Liquido
offers the possibility to run the automated end-2-end tests against PROD. When DevRestController.java is deployed
then you can fetch a JWT like this. You MUST pass the devLoginToken that is configured in appplication-prod.properties 

    http://{PROD_HOST}:{PROD_API_PORT}/liquido/v2/dev/getJWT?mobile=%2B491234567890&token=<devLoginToken>

# Logging

"I am a logging fanatic" (tm)

 - REST requests are logged via DoogiesRequestLogger
 - Services methods have entry and exit logging on TRACE level.
 - All writing methods log an INFO

# Software Architecture

https://www.draw.io/#LLiquido%20Architecture


# GraphQL

The new team endpoint is implemented with GraphQL. We use [GraphQL-SPQR](https://github.com/leangen/graphql-spqr) to automatically create the GraphQL schema.

Sample GraphQL PST request payload. 
(Keep in mind: If you want to send this as an HTTP POST request, you have to wrap this into JSON like so: {"query": "<GraphQL query from below>" }  ! There is no tutorial anywhere that tells you that :-)

    // A sample query
    { getAllTeams { teamName, inviteCode, id, createdAt, updatedAt, members { id, email, profile { name } }} } 

    // A sample mutation
    mutation {
        createNewTeam(teamName: "GraphQLTEam", adminName: "John Admin", adminEmail: "john_admin@testliqu.com", adminMobilephone:"+4955512345") {
            id
            teamName
            inviteCode
            members {
                id
                email
            }
        }
    }




See also this tutorial: https://medium.com/@saurabh1226/getting-started-with-graphql-spqr-with-springboot-bb9d232053ec

## GraphQL and Security (Authentication via JWT)

See this really nice spring example: https://github.com/Blacktoviche/springboot-graphql-sqqr-jwt-demo/





# Databases, Data and our famous TestDataCreator


## In dev environment   spring.profiles.active=dev

Let Hibernate generate the database schema (from scratch). And also create a .sql script for that. 

`application-dev.yml`
        
    spring:
      jpa:
        generate-ddl: true
        hibernate:
          ddl-auto: validate   # or create-drop

      # In dev we can create a schema.sql script for initializing a database later in other environments
      # https://stackoverflow.com/questions/37648395/how-to-see-the-schema-sql-ddl-in-spring-boot
      # https://thoughts-on-java.org/standardized-schema-generation-data-loading-jpa-2-1/
      properties:
        javax:
          persistence:
            schema-generation:
              database:
                action: create    # Must be set if you still want hibernate to create the actual schema in the DB in addition to the SQL file dump.
              scripts:
                action: create
                create-target: build/liquido-db-schema.sql

## In int environment

`application-int.yml`





# HTTP over SSL, HTTPS with certificate

https://lightsail.aws.amazon.com/ls/docs/de_de/articles/amazon-lightsail-using-lets-encrypt-certificates-with-lamp










# Roadmap

 * MAYBE package-by-feature  http://www.javapractices.com/topic/TopicAction.do?Id=205  => and then invent microservices :-)






## Spring Security

 - [Spring Security Reference](http://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#test-method-withuserdetails)
 - [Tutorial Spring Security with Angular JS](https://spring.io/guides/tutorials/spring-security-and-angular-js/#_the_login_page_angular_js_and_spring_security_part_ii)
 - [Hello Spring Security with Boot](http://docs.spring.io/spring-security/site/docs/current/guides/html5//helloworld-boot.html#updating-your-dependencies)
 - [Spring Data JPA Auditing](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#auditing)
 - [Spring Data Rest - Security](http://docs.spring.io/spring-data/rest/docs/current/reference/html/#security)

## Good Spring Boot Resources

 - https://github.com/spring-projects/spring-data-examples
 - Book+++: High Performance Java Persistence: https://leanpub.com/high-performance-java-persistence?utm_source=blog&utm_medium=banner&utm_campaign=banner
 - [Great Hibernate Tutorials](https://vladmihalcea.com/tutorials/hibernate/)  about mappings and sequences

## One Time Token
 - [Custom token based authentication of REST services](https://www.future-processing.pl/blog/exploring-spring-boot-and-spring-security-custom-token-based-authentication-of-rest-services-with-spring-security-and-pinch-of-spring-java-configuration-and-spring-integration-testing/)
 - [Github: Passwordless Auth](https://github.com/creactiviti/spring-security-passwordless)


## Two factor authentication

 - [Two Factor Auth with Spring Security (by Baeldung](https://www.baeldung.com/spring-security-two-factor-authentication-with-soft-token)
 - Google Authenticator
 - QRCode generator https://github.com/kenglxn/QRGen  (built upon ZXING)

## Oauth 2.0

 - [Spring-Security 5.1 now natively supports OAuth 2.0](https://docs.spring.io/spring-security/site/docs/current/reference/html/new.html)
 - [Baeldung Oauth Tutorials Overview](https://www.baeldung.com/spring-security-oauth)
 - [Baeldung Tutorial Oauth with JWT](https://www.baeldung.com/spring-security-oauth-jwt)
 - [Very nice Sprint Boot example app for JWT](https://github.com/nydiarra/springboot-jwt)

 
