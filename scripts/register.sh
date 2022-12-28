#!/bin/bash
# shellcheck disable=SC2181

#if [ $# == 0 ]; then
#  echo "Usage: $0 --bucket-name <BUCKET_NAME>"
#  exit
#fi

region=$(aws configure get region)
if [ -z "$region" ]; then
  echo "No region configured, please configure a default region using aws configure."
  exit
fi


set_default=0


while test $# -gt 0
do
    case "$1" in
        --set-default)
           set_default=1
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

cat banner.txt

# Create Execution Role

echo Creating Execution Role...
role_stack_id=$(aws cloudformation create-stack --stack-name awsutility-cloudformation-commandrunner-execution-role-stack --template-body file://resource-role.yaml --capabilities CAPABILITY_IAM --query StackId --output text 2>> registration_logs.log)

if ! [ $? -eq 0 ]; then
    # shellcheck disable=SC2034
    #Check if any updates can be made if it already exists
    role_stack_id=$(aws cloudformation update-stack --stack-name awsutility-cloudformation-commandrunner-execution-role-stack --template-body file://resource-role.yaml --capabilities CAPABILITY_IAM --query StackId --output text 2>> registration_logs.log)
    if ! [ $? -eq 0 ]; then
        echo Execution role already exists, no changes to be made.
        echo Creating Execution Role skipped.
    else
        echo Execution role already exists
        echo Updating Execution Role...
    fi
fi

stack_progress=$(aws cloudformation describe-stacks --stack-name awsutility-cloudformation-commandrunner-execution-role-stack --query Stacks[0].StackStatus --output text)
#stack_progress="CREATE_IN_PROGRESS"
while [[ $stack_progress == *"IN_PROGRESS" ]]
do
   echo "Waiting for execution role stack to complete..."
   sleep 10
   stack_progress=$(aws cloudformation describe-stacks --stack-name awsutility-cloudformation-commandrunner-execution-role-stack --query Stacks[0].StackStatus --output text)
   if [[ $stack_progress == "CREATE_COMPLETE" ]] || [[ $stack_progress == "UPDATE_COMPLETE" ]]; then
    echo "Creating/Updating Execution Role complete."
   fi
   if [[ $stack_progress == "CREATE_FAILED" ]] || [[ $stack_progress == "ROLLBACK_COMPLETE" ]]; then
    echo "Execution role failed to create, check CloudFormation Stack awsutility-cloudformation-commandrunner-execution-role-stack for errors."
    exit 1
   fi
done

execution_role_arn=$(aws cloudformation describe-stacks --stack-name awsutility-cloudformation-commandrunner-execution-role-stack --query Stacks[0].Outputs[0].OutputValue --output text)


# Create Temporary S3 Bucket

bucket_name=$(uuidgen | tr '[:upper:]' '[:lower:]' | tr -d '-')

# Create a bucket always as it will be cleaned up after installation.
echo "Creating temporary S3 Bucket $bucket_name..."
mb_result=$(aws s3 mb s3://"$bucket_name" --region "$region" 2>&1)
if [ $? -eq 0 ]; then
    echo Creating temporary S3 Bucket "$bucket_name" complete.
else
    if [[ $mb_result == *"BucketAlreadyOwnedByYou"* ]]; then
        echo Error: Bucket already owned, this installation requires the creation of a new temporary S3 Bucket.
        exit 1
    else
        echo Creating temporary S3 Bucket "$bucket_name" failed, please try again.
        echo "$mb_result" >> registration_logs.log
        echo "$mb_result"
        exit 1
    fi
fi


# Configure S3 Bucket Policy

#set -e
echo "Configuring S3 Bucket Policy for temporary S3 Bucket" "$bucket_name"...
aws s3api put-bucket-policy --bucket "$bucket_name" --policy '{"Version":"2012-10-17","Statement":[{"Action":["s3:GetObject","s3:ListBucket"],"Effect":"Allow","Resource":["arn:aws:s3:::'"$bucket_name"'/*","arn:aws:s3:::'"$bucket_name"'"],"Principal":{"Service":"cloudformation.amazonaws.com"}}]}'
echo "Configuring S3 Bucket Policy for temporary S3 Bucket" "$bucket_name" complete.


echo Copying Schema Handler Package to temporary S3 Bucket "$bucket_name"...
aws s3 cp awsutility-cloudformation-commandrunner.zip s3://"$bucket_name"/ >> registration_logs.log
echo Copying Schema Handler Package to temporary S3 Bucket "$bucket_name" complete.

#CFN Registration

echo Creating CommandRunner Log Group called awsutility-cloudformation-commandrunner-logs2...
# shellcheck disable=SC2034
log_group=$(aws logs create-log-group --log-group-name awsutility-cloudformation-commandrunner-logs2 2>> registration_logs.log)
if ! [ $? -eq 0 ]; then
    echo "Command Runner Log Group already exists, no changes to be made."
    echo "Creating CommandRunner Log Group skipped."
else
    echo Creating CommandRunner Log Group complete.
fi

registration_token=$(aws cloudformation register-type --type RESOURCE --type-name AWSUtility::CloudFormation::CommandRunner --schema-handler-package s3://"$bucket_name"/awsutility-cloudformation-commandrunner.zip --query RegistrationToken --output text --execution-role-arn "$execution_role_arn" --logging-config LogRoleArn="$execution_role_arn",LogGroupName=awsutility-cloudformation-commandrunner-logs2)
echo "Registering AWSUtility::CloudFormation::CommandRunner to AWS CloudFormation..."
echo "RegistrationToken:" "$registration_token"

progress_status="IN_PROGRESS"
while [[ $progress_status == "IN_PROGRESS" ]]
do
   echo "Waiting for registration to complete..."
   sleep 15
   progress_status=$(aws cloudformation describe-type-registration --registration-token "$registration_token" --query ProgressStatus --output text)
   if [[ $progress_status == "COMPLETE" ]]; then
    echo "Registering AWSUtility::CloudFormation::CommandRunner to AWS CloudFormation complete."
    if [ $set_default -eq 1 ]; then
          echo "Setting current version as default..."
          build_version_number=$(aws cloudformation describe-type-registration --registration-token "$registration_token" --query TypeVersionArn --output text | cut -d "/" -f4)
          aws cloudformation set-type-default-version --type RESOURCE --type-name AWSUtility::CloudFormation::CommandRunner --version-id "$build_version_number"
          echo "Setting current version as default complete." "(Current Version is" "$build_version_number"")"
    fi

   fi
   if [[ $progress_status == "FAILED" ]]; then
    echo "Type registration failed."
    aws cloudformation describe-type-registration --registration-token "$registration_token"
   fi
done

# Clean up temporary S3 Bucket
echo "Cleaning up temporary S3 Bucket..."
echo "Deleting SchemaHandlerPackage from temporary S3 Bucket $bucket_name..."
rm_result=$(aws s3 rm s3://"$bucket_name"/awsutility-cloudformation-commandrunner.zip 2>&1)
if [ $? -eq 0 ]; then
    echo "Deleting SchemaHandlerPackage from temporary S3 Bucket $bucket_name complete."
else
        echo Deleting SchemaHandlerPackage from temporary S3 Bucket "$bucket_name" failed, please delete it manually.
        echo "$rm_result" >> registration_logs.log
        echo "$rm_result"
        exit 1
fi
rb_result=$(aws s3 rb s3://"$bucket_name" 2>&1)
if [ $? -eq 0 ]; then
    echo Cleaning up temporary S3 Bucket complete.
else
        echo Cleaning up temporary S3 Bucket "$bucket_name" failed, please delete it manually.
        echo "$rb_result" >> registration_logs.log
        echo "$rb_result"
        exit 1
fi

echo ""
echo "AWSUtility::CloudFormation::CommandRunner is ready to use."
echo ""

exit 0
