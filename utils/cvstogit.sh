#!/bin/bash
#
# This script is used to convert an CVS repository to
# a git repository.
# Requirements:
# Have cvps installed and git-core contains git-cvsimport command
#

# Check to see if the argument was supplied.
# If not then exit violently.
if [ $# -eq 0 ]
then
echo "$0 : You must supply the CVS repository URL"
exit 1
fi

# Make the conversion here
echo "Converting the CVS repository at:" + $1
echo "NOTE: only include the URL up to the CVSROOT folder (which usually the top level folder)."

# $2 is the CVS Module. Basically it's the top level folder name 
# Arguments: <CVSROOT path> <Module name>
# Example: ./cvstogit.sh :pserver:anonymous@cvs.savannah.gnu.org:/web/url url

git cvsimport -p -x -v -d $1 -C ../temp/cvsConverted $2
exit 0