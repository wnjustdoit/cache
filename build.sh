#!/usr/bin/env bash
echo 'building is starting...'
cd cache-api/ && mvn clean install deploy
cd ../cache-redis/ && mvn clean install deploy
cd ../cache-redis-integration-spring/ && mvn clean install deploy