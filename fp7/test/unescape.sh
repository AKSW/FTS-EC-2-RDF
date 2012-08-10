#!/bin/bash

# Unescapes strings which were encoded with: backslash '\\', newline '\n' and tabs '\t'
sed -r 's|([^\\](\\\\)*)(\\n)|\1\n|g' | sed -r 's|([^\\](\\\\)*)(\\t)|\1\t|g' | sed -r 's|\\\\|\\|g'
