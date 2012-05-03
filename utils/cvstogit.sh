#!/bin/bash
#
# This script is used to convert an CVS repository to
# a git repository.
#
# Requirements:
# Have Python 2.X installed
# Have RCS and CVS installed
# Have Git 1.5.4.4 or later installed
#

# Check to see if the argument was supplied.
# If not then exit violently.
if [ $# -eq 0 ]
then
echo "$0 : You must supply the CVS repository path."
exit 1
fi

# Make the conversion here
# $1 is the CVS local repo.
echo "Converting the CVS repository at:" $1
cd cvs2svn-2.3.0
./cvs2git --blobfile=cvs2svn-tmp/git-blob.dat --dumpfile=cvs2svn-tmp/git-dump.dat --username=cvs2git $1
cd ..

# Initialize the new git repo
git init --bare --separate-git-dir ../temp/cvsConverted

#Load the dump files into the git repo
cd ../temp/cvsConverted
git fast-import --export-marks=../../cvs2svn-2.3.0/cvs2svn-tmp/gitmarks.dat < ../../cvs2svn-2.3.0/cvs2svn-tmp/git-blob.dat
git fast-import --import-marks=../../cvs2svn-2.3.0/cvs2svn-tmp/gitmarks.dat < ../../cvs2svn-2.3.0/cvs2svn-tmp/git-dump.dat

# Thats all folks
exit 0
