#!/bin/bash
#
# This script is used to convert an CVS repository to
# a git repository.
# Requirements:
# Have cvps installed and git-core contains git-cvsimport command
#

# Check to see if the argument was supplied.
# If not then exit violently.
if [ $# -lt 2 ]
then
echo "$0 : You must supply the CVS repository path as the first argument."
echo "$0 : You must supply the module name (CVSROOT)."
exit 1
fi

# Make the conversion here
echo "Converting the CVS repository at:" + $1
echo "NOTE: only include the path up to, but not including, the CVSROOT folder (which usually the top level folder). Specify the module as CVSROOT."

# $2 is the CVS Module. Basically it's the top level folder name 
# Arguments: <CVSROOT path> <Module name>
# Example (Remote): ./cvstogit.sh :pserver:anonymous@cvs.savannah.gnu.org:/web/url url
# Example (Local) : ./cvstogit.sh /path/to/local/cvs CVSROOT

git cvsimport -p -x -v -d $1 -C ../temp/cvsConverted $2
exit 0
