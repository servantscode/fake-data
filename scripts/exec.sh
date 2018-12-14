#!/bin/bash

echo "Making fake data..."
cd /usr/local/fake-data/lib
ls
java -jar fake-data.jar
echo "Done!"
