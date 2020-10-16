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
  Command:
    Type: 'AWSUtility::CloudFormation::CommandRunner'
    Properties:
      Command: aws s3 ls > /command-output.txt
      Role: String
      LogGroup: String #Optional
      SubnetId: String #Optional
      SecurityGroupId: String #Optional
      KeyId: String #Optional

Outputs:
    Output:
        Description: The output of the CommandRunner.
        Value: !GetAtt Command.Output
```

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
```

---

# User Installation Steps

*Note: To build the source yourself, see the `Developer Build Steps` section below.*

**Step 1**: Use the `register.sh` bash script to register resource from scratch and upload package to S3 bucket.

```text
$ ./scripts/register.sh
```

Below is an example of a successful registration using the `register.sh` script.

```text
$ ./scripts/register.sh
Creating Execution Role...
Waiting for execution role stack to complete...
Waiting for execution role stack to complete...
Creating Execution Role complete.
Creating temporary S3 Bucket f6f8e134b202493297d801183777d92f...
Creating temporary S3 Bucket f6f8e134b202493297d801183777d92f complete.
Configuring S3 Bucket Policy for temporary S3 Bucket f6f8e134b202493297d801183777d92f...
Configuring S3 Bucket Policy for temporary S3 Bucket f6f8e134b202493297d801183777d92f complete.
Copying Schema Handler Package to temporary S3 Bucket f6f8e134b202493297d801183777d92f...
Copying Schema Handler Package to temporary S3 Bucket f6f8e134b202493297d801183777d92f complete.
Registering AWSUtility::CloudFormation::CommandRunner to AWS CloudFormation...
RegistrationToken: 0cd1187e-0a27-405e-8df6-6136605031ee
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Waiting for registration to complete...
Registering AWSUtility::CloudFormation::CommandRunner to AWS CloudFormation complete.
Cleaning up temporary S3 Bucket...
Deleting SchemaHandlerPackage from temporary S3 Bucket f6f8e134b202493297d801183777d92f...
Deleting SchemaHandlerPackage from temporary S3 Bucket f6f8e134b202493297d801183777d92f complete.
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
        "Command": {
            "Type": "AWSUtility::CloudFormation::CommandRunner",
            "Properties": {
                "Command": "String",
                "Role": "String",
                "LogGroup": "String",           
                "SubnetId": "String",
                "SecurityGroupId" "String",
                "KeyId" "String"
            }
        }
    }
}
```

## YAML

```yaml
Resources:
  Command:
    Type: 'AWSUtility::CloudFormation::CommandRunner'
    Properties:
      Command: String
      Role: String
      LogGroup: String #Optional
      SubnetId: String #Optional
      SecurityGroupId: String #Optional
      KeyId: String #Optional

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
   Every command needs to output the desired value into the reserved file "/command-output.txt" like the following example.
         
   `aws s3 ls > /command-output.txt`
   
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

   The Id of the Subnet to execute the command in.
   
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

---

# Return Values

### Ref

Users can reference the output of the command outputted to /command-output.txt using Fn::GetAtt like in the following syntax.

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

The recommended versions and dependencies are as below.
```
aws-cli/1.16.209 and above.
Python/2.7.16 Darwin/18.7.0 botocore/1.13.30
Apache Maven 3.6.3 and above.
Java version: 13.0.2
cfn 0.1.2
jq-1.6

```

---

# Change Log

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

We are currently in the process of writing an AWS Blog about this resource as well as creating an AWS Samples repository for solutions created using this resource.