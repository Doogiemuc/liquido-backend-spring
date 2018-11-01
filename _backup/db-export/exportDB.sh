#!/bin/sh
#
# Export the collections from the mlab mongoDB into plain JSON files
#

echo "Exporting collections ..."

for collection in areas ballots delegations ideas laws users; do
  mongoexport --host ds019664.mlab.com --port 19664 --db liquido-test --username testuser --collection $collection --out ${collection}.json
done

echo "Done."
