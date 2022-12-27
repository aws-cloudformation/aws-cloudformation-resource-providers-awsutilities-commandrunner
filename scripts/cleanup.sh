#!/bin/bash

region=$(aws configure get region)
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


# Delete Execution Role

stack_name='awsutility-cloudformation-commandrunner-execution-role-stack'

echo "Checking if Execution Role exists..."

describe_result=$(aws cloudformation describe-stacks --stack-name $stack_name 2>&1)

if ! $describe_result; then
    echo "Execution role does not exist."
    echo "Deleting Execution Role skipped."
else
    echo "Execution role exists."
    echo "Deleting Execution Role..."
    result=$(aws cloudformation delete-stack --stack-name $stack_name 2>&1)
    if $result; then
        echo "Deleting Execution Role complete."
    else
        echo "$result"
    fi
fi

# Deregister Type


type_name='AWSUtility::CloudFormation::CommandRunner'

version='00000001'
count=1

while [ "$version" != "00000099" ]
do
    echo "Deregistering version $version..."
    command=$(aws cloudformation deregister-type --version-id "$version" --type RESOURCE --type-name $type_name --region "$region" 2>&1)
    if $command; then
        echo "Deregistering version $version complete".
    else
        if [[ $command == *"error"* ]]; then
            aws cloudformation deregister-type --type RESOURCE --type-name $type_name --region "$region"
            echo Successfully deregistered $type_name from region "$region".
            exit 0
        else
            echo "$command"
            exit 1
        fi
    fi
    count=$(("$count" + 1))
    if [ $count -lt 10 ]; then
        version='0000000'$count
    fi
    if [ $count -ge 10 ]; then
        version='000000'$count
    fi
done
