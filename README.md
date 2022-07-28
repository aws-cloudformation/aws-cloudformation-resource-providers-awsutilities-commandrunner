# AWSUtility::CloudFormation::CommandRunner

---

# **Table Of Contents**
- [Introduction](#introduction)
- [Prerequisites](#prerequisites)
- [User Installation Steps](#user-installation-steps)
- [Documentation](#documentation)
    - [Syntax](#syntax)
    - [Properties](#properties)
    - [Return Values](#return-values)
- [User Guides](#user-guides)
    - [Run a Command before or after any Resource](#run-a-command-before-or-after-any-resource)
    - [Run a script in any programming language using any SDK](#run-a-script-in-any-programming-language-using-any-sdk)
    - [Install Packages before Running Command](#install-packages-before-running-command)
    - [Use Cases](#use-cases)
- [FAQ](#faq)
- [Developer Build Steps](#developer-build-steps)
- [Change Log](#change-log)
- [See Also](#see-also)

---

# Introduction

AWSUtility::CloudFormation::CommandRunner is a CloudFormation resource type created using the recently released CloudFormation Resource Providers framework.

The AWSUtility::CloudFormation::CommandRunner resource allows users to run Bash commands in any CloudFormation stack.

This allows for unlimited customization such as executing AWS CLI/API calls, running scripts in any language, querying databases, doing external REST API calls, cleanup routines, validations, dynamically referencing parameters and just about anything that can be done using the shell on an EC2 instance.

The `AWSUtility::CloudFormation::CommandRunner` resource runs any command provided to it before or after any resource in the Stack.

`AWSUtility::CloudFormation::CommandRunner` can be used to perform inside your CloudFormation stack, any API call, script, custom logic, external check, conditions, cleanup, dynamic parameter retrieval and just about anything that can be done using a command.

Any output written using the command to the reserved file `/command-output.txt` can be referenced anywhere in your template by using `!Fn::GetAtt Command.Output` like below, where `Command` is the logical name of the `AWSUtility::CloudFormation::CommandRunner`resource.

```yaml
Resources:
  CommandRunner:
    Type: 'AWSUtility::CloudFormation::CommandRunner'
    Properties:
      Command: aws s3 ls | sed -n 1p | cut -d " " -f3 > /command-output.txt
      Role: String #Optional
      LogGroup: String #Optional
      SubnetId: String #Optional
      SecurityGroupId: String #Optional
      KeyId: String #Optional
      Timeout: String #Optional **NEW**
      DisableTerminateInstancesCheck: String #Optional **NEW**
      InstanceType: #Optional **NEW**

Outputs:
    Output:
        Description: The output of the CommandRunner.
        Value: !GetAtt Command.Output
```
*Note: In the above example, `sed -n 1p` prints only the first line from the response returned by `aws s3 ls`. To get the bucket name, `sed -n 1p` pipes the response to `cut -d " " -f3`, which chooses the third element in the array created after splitting the line delimited by a space.*

Only the property `Command` is required, while `Role`, `LogGroup`, `SubnetId` and `SecurityGroupId` are not required and have defaults.

`Command` is the Bash command.
`Role` is the IAM Role to run the command.
`LogGroup` is the CloudWatch Log Group to send logs from the command's execution.
`SubnetId` is the ID of the Subnet that the command will be executed in.
`SecurityGroupId` is the ID of the Security Group applied during the execution of the command.

For more information about the above properties, navigate to [Properties](#properties) in the [Documentation](#documentation).

_Note that the command once executed cannot be undone. It is highly recommended to test the AWSUtility::CloudFormation::CommandRunner resource out in a test stack before adding it to your production stack._

---

# Prerequisites

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
- To register the `AWSUtility::CloudFormation::CommandRunner` resource to your AWS account/region, you need IAM permissions to perform the following actions.

```
s3:CreateBucket
s3:DeleteBucket
s3:PutBucketPolicy
s3:PutObject
cloudformation:RegisterType
cloudformation:DescribeTypeRegistration
iam:createRole
logs:CreateLogGroup
```

---

# User Installation Steps

*Note: To build the source yourself, see the `Developer Build Steps` section below.*

**Step 0**: Clone this repository and download the latest release using the following commands.

```text
git clone https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-awsutilities-commandrunner.git
cd aws-cloudformation-resource-providers-awsutilities-commandrunner
curl -LO https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-awsutilities-commandrunner/releases/latest/download/awsutility-cloudformation-commandrunner.zip
```

**Step 1**: Use the `register.sh` bash script to register resource from scratch and upload package to S3 bucket. Pass the optional `--set-default` option to set this version to be the default version for the `AWSUtility::CloudFormation::CommandRunner` resource.

```text
$ ./scripts/register.sh --set-default
```

Below is an example of a successful registration using the `register.sh` script.

```text
$ ./scripts/register.sh 
Creating Execution Role...
Waiting for execution role stack to complete...
Waiting for execution role stack to complete...
Waiting for execution role stack to complete...
Waiting for execution role stack to complete...
Creating/Updating Execution Role complete.
Creating temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2...
Creating temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2 complete.
Configuring S3 Bucket Policy for temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2...
Configuring S3 Bucket Policy for temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2 complete.
Copying Schema Handler Package to temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2...
Copying Schema Handler Package to temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2 complete.
Creating CommandRunner Log Group called awsutility-cloudformation-commandrunner-logs2...
Creating CommandRunner Log Group complete.
Registering AWSUtility::CloudFormation::CommandRunner to AWS CloudFormation...
RegistrationToken: 0ae0622e-af3d-463b-9b2d-1d1e5fa41d14
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Registering AWSUtility::CloudFormation::CommandRunner to AWS CloudFormation complete.
Cleaning up temporary S3 Bucket...
Deleting SchemaHandlerPackage from temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2...
Deleting SchemaHandlerPackage from temporary S3 Bucket 7c96b969af1c41bfb2bd10f552255ca2 complete.
Cleaning up temporary S3 Bucket complete.

AWSUtility::CloudFormation::CommandRunner is ready to use.
```

The `register.sh` script performs the following operations to register the `AWSUtility::CloudFormation::CommandRunner` resource.

- Creates an Execution Role for the `AWSUtility::CloudFormation::CommandRunner` resource to give it permissions to perform the following actions.

```yaml
cloudformation:DeleteStack
cloudformation:CreateStack
cloudformation:DescribeStacks

logs:CreateLogStream
logs:DescribeLogGroups
logs:PutLogEvents

cloudwatch:PutMetricData

ssm:GetParameter
ssm:PutParameter
ssm:DeleteParameter

ec2:DescribeSubnets
ec2:DescribeVpcs
ec2:DescribeSecurityGroups
ec2:CreateSecurityGroup
ec2:RevokeSecurityGroupEgress
ec2:RevokeSecurityGroupIngress
ec2:CreateTags
ec2:AuthorizeSecurityGroupIngress
ec2:AuthorizeSecurityGroupEgress
ec2:RunInstances
ec2:DescribeInstances
ec2:TerminateInstances
ec2:DeleteSecurityGroup

iam:PassRole
iam:GetInstanceProfile
iam:SimulatePrincipalPolicy

#Only required if using the KeyId property, i.e custom KMS Key for the SSM SecureString
kms:Encrypt
kms:Decrypt

sts:GetCallerIdentity
```

- Runs the `aws s3 mb` AWS CLI command to create an S3 bucket with the name specified.
- Runs the `aws s3api put-bucket-policy` AWS CLI command to put the following bucket policy on the new bucket, where `<BUCKET_NAME>` is the specified bucket name.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Effect": "Allow",
            "Resource": [
                "arn:aws:s3:::<BUCKET_NAME>/*",
                "arn:aws:s3:::<BUCKET_NAME"
            ],
            "Principal": {
                "Service": "cloudformation.amazonaws.com"
            }
        }
    ]
}
```

- Runs the `aws s3 cp` AWS CLI command to copy the SchemaHandlerPackage `awsutility-cloudformation-commandrunner.zip` into the S3 bucket.
- Runs the `aws logs create-log-group` AWS CLI command to create the Log Group or skip it if it already exists.
- Runs the `aws cloudformation register-type` AWS CLI command to register the `AWSUtility::CloudFormation::CommandRunner` resource to CloudFormation.
- Cleans up the temporary S3 bucket created during registration.

Note that to run the `register.sh` script the IAM Role/User configured in the AWS CLI should have the following IAM permissions.

```
s3:CreateBucket
s3:DeleteBucket
s3:PutBucketPolicy
s3:PutObject
cloudformation:RegisterType
cloudformation:DescribeTypeRegistration
iam:CreateRole
```

The following sample policy can be attached to the IAM User/Role if they do not have the necessary permissions.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cloudformation:RegisterType",
                "cloudformation:DescribeTypeRegistration"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:CreateBucket",
                "s3:DeleteBucket",
                "s3:PutBucketPolicy",
                "s3:PutObject"
            ],
            "Resource": "*"
        }
    ]
}
```

---

# Documentation

# Syntax

## JSON

```json
{
    "Resources": {
        "CommandRunner": {
            "Type": "AWSUtility::CloudFormation::CommandRunner",
            "Properties": {
                "Command": "String",
                "Role": "String",
                "LogGroup": "String",
                "SubnetId": "String",
                "SecurityGroupId": "String",
                "KeyId": "String",
                "Timeout": "String",
                "DisableTerminateInstancesCheck": "String",
                "InstanceType": "String"           
            }
        }
    }
}
```

## YAML

```yaml
Resources:
  CommandRunner:
    Type: 'AWSUtility::CloudFormation::CommandRunner'
    Properties:
      Command: String
      Role: String #Optional
      LogGroup: String #Optional
      SubnetId: String #Optional
      SecurityGroupId: String #Optional
      KeyId: String #Optional
      Timeout: String #Optional **NEW**
      DisableTerminateInstancesCheck: String #Optional **NEW**
      InstanceType: #Optional **NEW**

Outputs:
    Output:
        Description: The output of the CommandRunner.
        Value: !GetAtt Command.Output

```
---

# Properties

### Command

   The bash command that you would like to run.

   For AWS CLI commands, please specify the region using the --region option.

   #### Note:
   Every command needs to output the desired value into the reserved file "/command-output.txt" like the following example. The value written to the file must be a non-empty single word value without quotation marks like `vpc-0a12ab123abc9876` as they are intended to be used inside the CloudFormation template using `Fn::GetAtt`.

   `aws ec2 describe-vpcs --query Vpcs[0].VpcId --output text  > /command-output.txt`

   #### Note:
   The command is run on the latest Amazon Linux 2 AMI in your region.

   _Required_: Yes

   _Type_: String

   _Update requires_: Replacement

### Role

   The IAM Instance Profile to be used to run the Command. The Role in the Instance Profile will need all the permissions required to run the above `Command`.

   #### Note:
   The Role should have permissions to perform the actions below to write logs to CloudWatch from the command's execution.
   ```
   "logs:CreateLogStream",
   "logs:CreateLogGroup",
   "logs:PutLogEvents"
   ```

   If the Role does not have the above logging permissions, the command will still work but no logs will be written.

   #### Note:
   The Role in the Instance Profile should specify `ec2.amazonaws.com` as a Trusted Entity.
   An Instance Profile is created automatically when a Role is created using the Console for an EC2 instance.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement

### LogGroup

   The CloudWatch Log Group to stream the logs from the specified command.

   If one is not provided the default `cloudformation-commandrunner-log-group` one will be used.

   If the specified log group does not exist, a new one will be created.

   #### Tip:
   To log a trace of your commands and their arguments after they are expanded and before they are executed, run `set -xe` in the `Command` property before your actual command.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement

### SubnetId

   The Id of the Subnet to execute the command in. Note that the SubnetId specified should have access to the internet to be able to communicate back to CloudFormation. Ensure that the Route Table associated with the Subnet has a route to the internet via either an Internet Gateway (IGW) or a NAT Gateway (NGW).

   #### Note:
   If the `SubnetID` is not specified, it will create the resource in a subnet in the default VPC of the region.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement

### SecurityGroupId

   The Id of the Security Group to attach to the instance the command is run in. If using SecurityGroup, the SubnetId property is required.

   #### Note:
   If the `SecurityGroupId` is not specified, the command will be run with a security group with open Egress rules and no Ingress rules.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement

### KeyId

   Id of the KMS key to use when encrypting the output stored in SSM Parameter Store. If not specified, the account's default KMS key is used.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement
   
### Timeout

   By default, the timeout is 600 seconds. To increase the timeout specify a higher Timeout value in seconds. The maximum timeout value is 43200 seconds i.e 12 hours.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement

### DisableTerminateInstancesCheck

   By default, CommandRunner checks to see if the execution role can perform a TerminateInstances API call. Set this property to true if you want to skip the check. Note that this means that the CommandRunner instance may not be terminated and will have to be terminated manually.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement

### InstanceType

   By default, the instance type used is t2.medium. However you can use this property to specify any supported instance type.

   _Required_: No

   _Type_: String

   _Update requires_: Replacement

---

# Return Values

### Fn::GetAtt

Users can reference the output of the command written to `/command-output.txt` using `Fn::GetAtt` like in the following syntax.

```yaml
Outputs:
    Output:
        Description: The output of the command.
        Value: !GetAtt Command.Output
```

---

# User Guides

## Run A Command Before Or After A Resource

To run the command after a resource with logical name `Resource`, specify `DependsOn: Resource` in the AWSUtility::CloudFormation::CommandRunner resource's definition.

```yaml
Resources:
   Command:
      DependsOn: Resource
      Type: AWSUtility::CloudFormation::CommandRunner
      Properties:
         Command: aws s3 ls > /command-output.txt
         LogGroup: my-cloudwatch-log-group
         Role: EC2-Role
   Resource:
      Type: AWS::EC2::Instance
      Properties:
         Image: ami-abcd1234
```

To run the command before a resource, put a `DependsOn` with the logical name of the AWSUtility::CloudFormation::CommandRunner resource in that resource's definition.

```yaml
Resources:
   Command:
      Type: AWSUtility::CloudFormation::CommandRunner
      Properties:
         Command: aws s3 ls > /command-output.txt
         LogGroup: my-cloudwatch-log-group
         Role: EC2-Role
   Resource:
      DependsOn: Command
      Type: AWS::EC2::Instance
      Properties:
         Image: ami-abcd1234
```

## Run a script in any programming language using any SDK

You can write a script in any programming language and upload it to S3. Use the `aws s3 cp` command to copy the script from S3 followed by `&&` and the command to run the script like the following example.

```yaml
Resources:
    Command:
        Type: AWSUtility::CloudFormation::CommandRunner
        Properties:
            Command: 'aws s3 cp s3://cfn-cli-project/S3BucketCheck.py . && python S3BucketCheck.py my-bucket third-name-option-a'
            Role: EC2AdminRole
            LogGroup: my-cloudwatch-log-group
Outputs:
    Output:
        Description: The output of the command.
        Value: !GetAtt Command.Output
```

## Install Packages before Running Command

```yaml
Resources:
    Command:
        Type: AWSUtility::CloudFormation::CommandRunner
        Properties:
            Command: 'yum install jq -y && aws ssm get-parameter --name RepositoryName --region us-east-1 | jq -r .Parameter.Value > /command-output.txt'
            Role: EC2AdminRole
            LogGroup: my-cloudwatch-log-group
Outputs:
    Output:
        Description: The output of the command.
        Value: !GetAtt Command.Output
```

# Use Cases

- The AWSUtility::CloudFormation::CommandRunner resource lets you perform any API call, script, custom logic, external check, conditions, cleanup, dynamic parameter retrieval and anything else that can be done using a command.

- Get parameters dynamically during the Stack's execution instead of passing in Parameters during stack creation.
  - Currently, Dynamic Referencing i.e SSM {{resolve}} on CloudFormation cannot automatically get the latest version of the SSM Parameter. Due to this, users have to know the latest version number and manually put it in every time or the CFN stack will continue to resolve to the old version. This can be worked around using AWSUtility::CloudFormation::CommandRunner to always get the latest parameter value.

- Currently, there is no AWS::ECS resource that allows you to configure the Account Settings. However, you can do this using the AWSUtility::CloudFormation::CommandRunner resource by running the `aws ecs put-account-setting` CLI commmand.

- Currently, there is no way to create an image (AMI) using a running EC2 instance, but it can be done using the AWSUtility::CloudFormation::CommandRunner resource by using the `aws ec2 create-image` CLI command.

- Add a lag between resources using CommandRunner. Specify a sleep command in the Command property.

---

# FAQ

#### Q. Why use EC2 instead of Lambda?

 - Lambda does not natively support using Bash.

 - Even if Bash is added using custom Lambda Layers it will still not allow installing new packages or running other programming / scripting languages.

 - Lambda is also more expensive than running a small EC2 instance for approximately 2 minutes.

#### Q. Why make this when Custom Resources and Macros are available?

 - Developing a Custom Resource or Macro requires writing several lines of code and troubleshooting which is considered to be development effort.

 - The AWSUtility::CloudFormation::CommandRunner provides a quick fix to solve problems with a single line of code and does not require any development effort.

 - Using the AWSUtility::CloudFormation::CommandRunner, users can quickly unblock themselves without relying on CloudFormation to resolve the issue to unblock them.

---

# Developer Build Steps

Please execute the included build script by running `./scripts/build.sh` to build and register the resource to AWS CloudFormation for your account. The script waits while CloudFormation registers the resource so it typically takes about 5-10 minutes.

Note that the script assumes that you have AWS CLI configured and the necessary permissions to register a resource provider to CloudFormation, jq, mvn and the prerequisites mentioned here. `https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/what-is-cloudformation-cli.html`

The build script will do the following:

1. Runs `cfn generate` to generate the rpdk files for the Resource Provider.

2. `mvn package` packages the Java code up and `cfn submit` registers the built Resource to AWS CloudFormation in your AWS account.

3. From the output of the `cfn submit` command, it gets the version of the build and updates the default version to be used in CloudFormation.

Once the script finishes, the AWSUtility::CloudFormation::CommandRunner resource will be ready to use.

You can find an example of how to use the resource in the file `usage-template.yaml`.

As of July 2022, the recommended versions and dependencies for the build are as follows.
```
$ cfn --version
cfn 0.2.24

$ mvn -version
Apache Maven 3.6.3 (cecedd343002696d0abb50b32b541b8a6ba2883f)
Maven home: /Users/shantgup/Downloads/mvn
Java version: 15.0.2, vendor: AdoptOpenJDK, runtime: /Library/Java/JavaVirtualMachines/adoptopenjdk-15.jdk/Contents/Home
Default locale: en_US, platform encoding: UTF-8
OS name: "mac os x", version: "10.16", arch: "x86_64", family: "mac"

$ java -version
openjdk version "15.0.2" 2021-01-19
OpenJDK Runtime Environment AdoptOpenJDK (build 15.0.2+7)
OpenJDK 64-Bit Server VM AdoptOpenJDK (build 15.0.2+7, mixed mode, sharing)

$ ./scripts/build.sh
```

---

# Change Log

### v2.0

* Updated package versions in pom.xml to latest, fixing build issues related to outdated dependencies.
* Improved Error Handling 
    * For when command fails or when invalid value is written to `/command-output.txt`
        * Error message about checking cloud-init-output.log also includes network related issues.
    * When no default VPC exists.
        * Added try catch block for catching exception if no default VPC.
        * Error message - "No default VPC found in this region, please specify a subnet using the SubnetId property."
* Improved logging
    * Added contents of  `/command-output.txt` to CloudWatch logs under `cloud-init-output.log`.
    * Updated BaseTemplate to add contents of /command-output.txt to cloudwatch logs.
* Added catch for failures on CommandRunner stack.
    * Failure on CommandRunner stack was not being caught when response sent to CommandRunner stack in WaitCondition is malformed.
    * Failures are now caught right away, if CommandRunner stack goes into ROLLBACK_COMPLETE, or ROLLBACK_FAILED, then it will now gracefully clean up the CommandRunner stack.
* Updated user installation script `register.sh`
    * Added creation of log group in `register.sh`, along with handling the case where it already exists.
    * LogGroup not created for new region, line 103 of register.sh check if log-group exists if not, create one.
    * register.sh will try to create a fresh execution role stack, if it exists it will try to update it, if it is up to date it will skip it.
* Fixed bugs with networking configuration properties i.e `SubnetId`, `SecurityGroupId`
    * Removed empty string checks, now all different scenarios with/without SubnetId, SecurityGroupId work.
* Added new `Timeout` property.
    * Timeout property to change timeout in WaitCondition in BaseTemplate, this will give the option to easy fail, by default timeout is 600 right now, this will allow for a max timeout of 12 hours i.e 43200
* Added new `DisableTerminateInstancesCheck` property.
    * Some users were running into issues where their SCP policies did not allow the `ec2:TerminateInstances` action, but they still want to create CommandRunner instances. Setting this property to true allows them to create CommandRunner instances even without the `ec2:TerminateInstances` action. 
* Added new `InstanceType` property.
* Now works in Private Subnets. We had seen some issues where CommandRunner wouldn't work in private subnets, this issue is now resolved. 
* Added check for Instance Profile validity. An error is thrown within 5 seconds of resource creation if the Role property specified is not a valid Instance Profile.
    * Check for Instance Profile validity, performs DescribeInstanceProfile and catches the error if it doesn’t exist.
        * https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/identitymanagement/model/GetInstanceProfileResult.html
        * https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/identitymanagement/model/GetInstanceProfileRequest.html
        * Error message: *“*The Role property specified is not a valid Instance Profile.”
* Added .gitignore to repository, removed unnecessary temporary files. 
* Documentation
    * Fixed typos and grammatical errors.
    * Added new properties to all examples and schemas.
    * Added new properties to documentation.
    * Added new permissions to documentation.
* Fixed a bug where a fresh installation using register.sh wouldn’t work unless build.sh had been used before it. 


### v1.21

- Updated README to add more clarification into what values are accepted by `/command-output.txt`.
- Changed `cloudwatch:` to `logs:`, fixing the permissions issue when writing logs.
- Updated README to improve instructions for user installation steps.

### v1.2

- Output stored in SSM Parameter Store is now `SecureString` by default i.e Encrypted at rest using the Default KMS key of the account.
- Added new parameter `KMSKeyId` allowing users to specify their own customer-managed KMS Key to encrypt the SSM SecureString Parameter.
- Updated README with log permissions and specified that no error is thrown when it can’t write to log group. It requires the following permissions to write logs. If not provided, it won’t do any logging.
```
"logs:CreateLogStream",
"logs:CreateLogGroup",
"logs:PutLogEvents"
```
- The idea is that the command should still run even if the logs can’t be written. Users should have the option to not log if not required.

### v1.1

- Added `register.sh`and user build steps
- Added notes to the Properties
- Contract tests using `cfn test` all work with the following results.

```bash
collected 12 items / 5 deselected / 7 selected

handler_create.py::contract_create_delete PASSED                                                                                                [ 14%]
handler_create.py::contract_create_duplicate PASSED                                                                                             [ 28%]
handler_create.py::contract_create_read_success PASSED                                                                                          [ 42%]
handler_delete.py::contract_delete_read PASSED                                                                                                  [ 57%]
handler_delete.py::contract_delete_delete PASSED                                                                                                [ 71%]
handler_delete.py::contract_delete_create SKIPPED                                                                                               [ 85%]
handler_misc.py::contract_check_asserts_work PASSED                                                                                             [100%]
```

### v1.0

- Improved build script `build.sh`, it now does not use S3. The `BaseTemplate.json` is stored as a resource in the `.jar` file.
- Cleaned up code, removed comments, verbose code, etc.

### v0.9


- Removed all the extra build steps, now it takes only running the `build.sh` script after you’ve cloned the repo.
    - Previously, to build the project, the static variables in `CreateHandler.java` needed to be replaced. They are now dynamically inferred and replaced in `CreateHandler.java` using the `build.sh` script.
    - Added 2 new parameters and the Java logic to support them. Users can now optionally specify the `SubnetId` and the `SecurityGroupId` or both. If neither is provided it’ll use a subnet in the default VPC and create a SecurityGroup automatically.

- Instead of using `{{resolve}}`, `Fn::GetAtt` now works, you can do `!GetAtt Command.Output`

- Updated the presentation to reflect the above changes.

- Updated the documentation
    - New build steps and what `build.sh` does
    - Dependencies and versions used
    - Disclaimer about Command in docs
    - Properties SubnetId and SecurityGroupId
    - Referencing using Fn::GetAtt
    - Added Change History section

- If a security group is not provided, the one that is automatically created has no inbound rules and only allows outbound communication.

- Previously I had provided wildcard permissions to the resource. Now, only the below permissions are used and there are no wildcards.

```
cloudformation:DeleteStack
cloudformation:CreateStack
cloudformation:DescribeStacks

logs:CreateLogStream
logs:DescribeLogGroups

ssm:GetParameter
ssm:PutParameter

ec2:DescribeSubnets
ec2:DescribeVpcs
ec2:DescribeSecurityGroups
ec2:CreateSecurityGroup
ec2:RevokeSecurityGroupEgress
ec2:RevokeSecurityGroupIngress
ec2:CreateTags
ec2:AuthorizeSecurityGroupIngress
ec2:AuthorizeSecurityGroupEgress
ec2:RunInstances
ec2:DescribeInstances
ec2:TerminateInstances
ec2:DeleteSecurityGroup
iam:PassRole

#Only required if using the KeyId property, i.e custom KMS Key for the SSM SecureString
kms:Encrypt
kms:Decrypt
```

---

# See Also

[AWS Blogs - AWS Cloud Operations & Migrations Blog - Running bash commands in AWS CloudFormation templates](https://aws.amazon.com/blogs/mt/running-bash-commands-in-aws-cloudformation-templates/)

[AWS Premium Support - Knowledge Center - CloudFormation - How do I use AWSUtility::CloudFormation::CommandRunner to run a command before or after a resource in my CloudFormation stack?](https://aws.amazon.com/blogs/mt/running-bash-commands-in-aws-cloudformation-templates/)
