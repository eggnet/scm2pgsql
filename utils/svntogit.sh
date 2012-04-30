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
git svn clone -s $0 ../temp/svnConverted