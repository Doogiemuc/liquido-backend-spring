#!/bin/sh
java -Dspring.profiles.active=dev,local -Dliquido.test.loadTestData=true -jar target/liquido-backend-spring-4.1.0.jar