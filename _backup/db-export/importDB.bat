@echo off
rem
rem   Import data from json files into local mongoDB
rem

set PATH=%PATH%;C:\Coding\DevApps\MongoDB3.2.9\bin\

for %%c in (areas, ballots, delegations, ideas, laws, users) do (
  echo "Importing into collection '%%c'"
  mongoimport /host:localhost /port:27017 /db:liquido-test /collection:%%c /file:%%c.json /upsert
)

echo "you may now want to configure the DB:    mongo liquido-test configureDB.js"

echo "Done."