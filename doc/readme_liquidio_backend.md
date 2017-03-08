# Liquido-backend-spring Development Scratchpad

Here I write down everything I need during development of my little backend. 

## Process Model from an idea to a Law

 - At first there is an idea  (NEW_IDEA)
 - When an idea reaches the quorum, then it becomes a proposal. (INITIAL_PROPOSAL)
 - Alternative proposals can be added. But the need to reach a certain quorum too. (ALTERNATIVE_PROPOSAL)
 - Each proposal can be discussed (ELABORATION)
 - n days after the initial proposal reached its quorum, then the voting phase starts. 
   All competing proposals can be voted upon. (VOTING_PHASE)
 - The winning proposal becomes a law  (LAW) 

## Backlog / User Stories

### Profile

 - Make configurable
   - number of likes that are necessary to move an idea onto the table
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



# Architecture

https://www.draw.io/#LLiquido%20Architecture


## Links

 - [H2 DB Web Console](http://localhost:8080/h2-console)   JDBC URL: `jdbc:h2:mem:testdb`   Username: `sa` - no pwd, Driver Class: `org.h2.Driver`  
 - [Great Hibernate Tutorials](https://vladmihalcea.com/tutorials/hibernate/)  about mappings and sequences
 - Buch+++: High Performance Java Persistence: https://leanpub.com/high-performance-java-persistence?utm_source=blog&utm_medium=banner&utm_campaign=banner

## Spring Security
 - [Spring Security Reference](http://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#test-method-withuserdetails)
 - [Tutorial Spring Security with Angular JS](https://spring.io/guides/tutorials/spring-security-and-angular-js/#_the_login_page_angular_js_and_spring_security_part_ii)
 - [Hello Spring Security with Boot](http://docs.spring.io/spring-security/site/docs/current/guides/html5//helloworld-boot.html#updating-your-dependencies)
 - [Spring Data JPA Auditing](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#auditing)
 - [Spring Data Rest - Security](http://docs.spring.io/spring-data/rest/docs/current/reference/html/#security)
 

## Good Spring Boot Resources

 - https://springframework.guru/
 - https://springframework.guru/using-the-h2-database-console-in-spring-boot-with-spring-security/
 - baeldung
 
 
 ### Old conneetion to MongoDB
 
    # MongoDB via spring-data-mongo
    spring.data.mongodb.uri=mongodb://testuser:PASSWORD@ds019664.mlab.com:19664/liquido-test
    spring.data.mongodb.uri=mongodb://localhost:27017/liquido-test
    
    