currentPath=`pwd`;
echo $currentPath
# Loop for all Child Companies
echo "The number of arguments is: $#"
appHome=$1
for i in "${@:2}"; do
	echo "Running for company Id: $i"
	# Run liquibase command
	$currentPath/update-child.sh $appHome/descriptors/database/fc-client-data.xml $i
done
