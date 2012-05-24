#!/bin/bash

# Scan the log output for how many commitments were without a detail amount
grep "No detail amount" | sed -r 's/(.*)Main: (.*)/\2/g' | sort -u

