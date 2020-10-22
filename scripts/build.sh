set -e
cfn generate
mvn -Dmaven.test.skip=true package
cfn submit --set-default
echo 'AWSUtility::CloudFormation::CommandRunner ready to use.'
