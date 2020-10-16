region=`aws configure get region`
if [ -z "$region" ]; then
  echo "No region configured, please configure a default region using aws configure."
  exit
fi

while test $# -gt 0
do
    case "$1" in
        --region)
           region=$2
           ;;
#        --bucket-name) 
##          echo "Bucket name provided is" $2
#           bucket_name=$2
#           ;;
         --*) 
            echo "Not an option $1"
            ;;
#        *) echo "argument $1"
#           ;;
    esac
    shift
done

type_name='AWSUtility::CloudFormation::CommandRunner'

version='00000001'
count=1

while [ $version != "00000099" ]
do
    echo "Deregistering version "$version...
    command=`aws cloudformation deregister-type --version-id $version --type RESOURCE --type-name $type_name --region $region 2>&1`
    if [ $? -eq 0 ]; then
        echo "Deregistering version "$version complete.
    else
        if [[ $command == *"error"* ]]; then
            aws cloudformation deregister-type --type RESOURCE --type-name $type_name --region $region
            echo Successfully deregistered $type_name from region $region.
            exit 0
        else
            echo $command
            exit 1
        fi
    fi
    count=`expr $count + 1`
    if [ $count -lt 10 ]; then
        version='0000000'$count
    fi
    if [ $count -ge 10 ]; then
        version='000000'$count
    fi
done