#!/bin/bash
#
# This script is used to convert an SVN repository to
# a git repository.
#

# Check to see if the argument was supplied.
# If not then exit violently.
if [ $# -eq 0 ]
then
echo "$0 : You must supply the SVN repository URL"
exit 1
fi

# Make the conversion here
echo "Converting the SVN repository at:" + $1
echo "NOTE: only include the URL up to the /svn, do NOT include the /trunk at the end."
git svn clone -s $1 ../temp/svnConverted
exit 0
