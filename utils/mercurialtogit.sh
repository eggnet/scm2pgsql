#!/bin/bash
#
# This script is used to convert an Mercurial repository to
# a git repository.
#

# Check to see if the argument was supplied.
# If not then exit violently.
if [ $# -eq 0 ]
then
echo "$0 : You must supply the Mercurial repository URL"
exit 1
fi

# Make the conversion here
echo "Converting the Mercurial repository at:" $1
git init ../temp/mercurialConverted
cd ../temp/mercurialConverted
../../utils/hg-fast-export.sh -r $1
git checkout master
exit 0
