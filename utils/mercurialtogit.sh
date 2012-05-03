#!/bin/bash
#
# This script is used to convert Mercurial repository to
# a git repository.
#
# Requirements:
# Have Mercurial installed
# Have Git installed
#

# Check to see if the argument was supplied.
# If not then exit violently.
if [ $# -eq 0 ]
then
echo "$0 : You must supply the Mercurial repository path."
exit 1
fi

# Make the conversion here
# $1 is the CVS local repo.
echo "Converting the Mercurial repository at:" $1
cd ../temp/mercurialConverted
git init
../../utils/fast-export/hg-fast-export.sh -r $1
git checkout HEAD

# Thats all folks
exit 0
