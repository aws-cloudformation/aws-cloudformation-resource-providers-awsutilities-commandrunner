// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.awsutility.cloudformation.commandrunner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterResult;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.io.BufferedReader;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private static final String INSTANCE_TYPE = "t3.micro";

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        /*
         INFO: 'model' has all the properties from the CFN resource i.e Triggers, Command, Role, and LogGroup
         INFO: 'request' has information like region and AWS account ID
        */

        if (callbackContext == null) {

            AmazonCloudFormation stackbuilder = AmazonCloudFormationClientBuilder.standard()
                  .build();

            Random random = new Random();
            String generatedString = random.ints(97, 122 + 1)
                    .limit(10)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            String stackName           = "AWSUtility-CloudFormation-CommandRunner-"+generatedString;

            try {
                InputStream in = CreateHandler.class.getResourceAsStream("/BaseTemplate.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder out = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }

                CreateStackRequest createRequest = new CreateStackRequest();
                createRequest.setStackName(stackName);
                createRequest.setTemplateBody(out.toString());
                reader.close();
                System.out.println("Creating a stack called " + createRequest.getStackName() + ".");
                Collection<Parameter> parameters = new LinkedList<>();
                Parameter AMIId = new Parameter();
                AMIId.setParameterKey("AMIId");

                //Dynamically get latest Amazon Linux 2 AMI for the region
                  AWSSimpleSystemsManagement simpleSystemsManagementClient = ((AWSSimpleSystemsManagementClientBuilder.standard())).build();

                GetParameterRequest parameterRequest = new GetParameterRequest();
                parameterRequest.withName("/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2").setWithDecryption(Boolean.valueOf(true));
                GetParameterResult parameterResult = proxy.injectCredentialsAndInvoke(parameterRequest, simpleSystemsManagementClient::getParameter);
                String parameterValue = parameterResult.getParameter().getValue();

                AMIId.setParameterValue(parameterValue);
                parameters.add(AMIId);

                Parameter Command = new Parameter();
                Command.setParameterKey("Command");
                Command.setParameterValue(model.getCommand());
                parameters.add(Command);

                if (model.getRole() != null || model.getRole() == "") {
                    Parameter IamInstanceProfile = new Parameter();
                    IamInstanceProfile.setParameterKey("IamInstanceProfile");
                    IamInstanceProfile.setParameterValue(model.getRole());
                    parameters.add(IamInstanceProfile);
                }

                Parameter InstanceType = new Parameter();
                InstanceType.setParameterKey("InstanceType");
                //Note: HardCoded for now, will change in the future if the resource allows the customer to specify instance type.
                InstanceType.setParameterValue(INSTANCE_TYPE);
                parameters.add(InstanceType);

                if (model.getLogGroup() != null || model.getLogGroup() == "") {
                    Parameter LogGroup = new Parameter();
                    LogGroup.setParameterKey("LogGroup");
                    LogGroup.setParameterValue(model.getLogGroup());
                    parameters.add(LogGroup);
                }

                //Dynamically gets both vpcId and subnetId
                System.out.println(model.toString());
                if ((model.getSubnetId() == null && model.getSecurityGroupId() == null) ||
                        (model.getSubnetId() == "" && model.getSecurityGroupId() == "")) { //Check if user provided the subnetId, if not get a default.
                    System.out.println("Inside dynamic creation workflow!");
                    Parameter SubnetId = new Parameter();
                    SubnetId.setParameterKey("SubnetId");

                    Parameter VpcId = new Parameter();
                    VpcId.setParameterKey("VpcId");

                    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().build();
                    DescribeVpcsRequest describeVpcsRequest = new DescribeVpcsRequest();
                    describeVpcsRequest.withFilters(new Filter("isDefault").withValues("true"));
                    DescribeVpcsResult describeVpcsResult = proxy.<DescribeVpcsRequest,DescribeVpcsResult>injectCredentialsAndInvoke(describeVpcsRequest, ec2::describeVpcs);
                    String vpcId = describeVpcsResult.getVpcs().get(0).getVpcId();
                    if (vpcId == null || vpcId.isEmpty()) {
                        System.out.println("No default VPC found in this region, please specify a subnet using the NetworkConfiguration property.");
                        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .status(OperationStatus.FAILED)
                                .errorCode(HandlerErrorCode.InvalidRequest)
                                .message("No default VPC found in this region, please specify a subnet using the NetworkConfiguration property.")
                                .build();
                    }
                    VpcId.setParameterValue(vpcId);
                    DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
                    describeSubnetsRequest.withFilters(new Filter("vpc-id").withValues(vpcId));
                    DescribeSubnetsResult describeSubnetsResult = proxy.<DescribeSubnetsRequest,DescribeSubnetsResult>injectCredentialsAndInvoke(describeSubnetsRequest, ec2::describeSubnets);
                    String subnetId = describeSubnetsResult.getSubnets().get(describeSubnetsResult.getSubnets().size()-1).getSubnetId();
                    if (subnetId == null || subnetId.isEmpty()) {
                        System.out.println("Default VPC has no subnets. Please specify a subnet using the NetworkConfiguration property");
                        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .status(OperationStatus.FAILED)
                                .errorCode(HandlerErrorCode.InvalidRequest)
                                .message("Default VPC has no subnets. Please specify a subnet using the NetworkConfiguration property.")
                                .build();
                    }
                    SubnetId.setParameterValue(subnetId);
                    System.out.println("SubnetId=" + SubnetId);
                    parameters.add(SubnetId);
                    parameters.add(VpcId);
                }
                //Both are provided
                else if ((model.getSubnetId() != null && model.getSecurityGroupId() != null) ||
                        (model.getSubnetId() != "" && model.getSecurityGroupId() != "")) {
                    System.out.println("INSIDE BOTH ARE PROVIDED WORKFLOW.");
                    Parameter SubnetId = new Parameter();
                    SubnetId.setParameterKey("SubnetId");
                    SubnetId.setParameterValue(model.getSubnetId());
                    parameters.add(SubnetId);

                    Parameter SecurityGroupId = new Parameter();
                    SecurityGroupId.setParameterKey("SecurityGroupId");
                    //Note: HardCoded for now, will have to change in the future.
                    SecurityGroupId.setParameterValue(model.getSecurityGroupId());
                    parameters.add(SecurityGroupId);
                }
                //Subnet is provided, but not SecurityGroup. Infer VPC from Subnet and provide VPCId to CFN Stack
                else if ((model.getSubnetId() != null && model.getSecurityGroupId() == null) ||
                        (model.getSubnetId() != "" && model.getSecurityGroupId() == "")) {
                    System.out.println("INSIDE SUBNET PROVIDED NO SECURITY GROUP WORKFLOW.");
                    Parameter SubnetId = new Parameter();
                    SubnetId.setParameterKey("SubnetId");
                    SubnetId.setParameterValue(model.getSubnetId());
                    parameters.add(SubnetId);

                    Parameter VpcId = new Parameter();
                    VpcId.setParameterKey("VpcId");

                    AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().build();
                    DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
                    describeSubnetsRequest.withFilters(new Filter("subnet-id").withValues(model.getSubnetId()));
                    DescribeSubnetsResult describeSubnetsResult = proxy.<DescribeSubnetsRequest,DescribeSubnetsResult>injectCredentialsAndInvoke(describeSubnetsRequest, ec2::describeSubnets);
                    String vpcId = describeSubnetsResult.getSubnets().get(0).getVpcId();
                    VpcId.setParameterValue(vpcId);
                    parameters.add(VpcId);

                }

                else if ((model.getSubnetId() == null && model.getSecurityGroupId() != null) ||
                        (model.getSubnetId() == "") && model.getSecurityGroupId() != "") {
                    System.out.println("No SubnetId provided, when using SecurityGroupId, property SubnetId is required.");
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.FAILED)
                            .errorCode(HandlerErrorCode.InvalidRequest)
                            .message("No SubnetId provided, when using SecurityGroupId, property SubnetId is required.")
                            .build();
                }

                createRequest.setParameters(parameters);
                System.out.println(createRequest.getParameters().toString());
                //Inject creds and call instead
                proxy.injectCredentialsAndInvoke(createRequest, stackbuilder::createStack);

                //If CallbackContext coming in is null, always create stack and set OperationStatus.IN_PROGRESS
                model.setId(generatedString);
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .callbackContext(CallbackContext.builder().stackName(stackName).stackId(generatedString).build())
                        .status(OperationStatus.IN_PROGRESS)
                        .callbackDelaySeconds(90)
                        .build();

            } catch (AmazonServiceException ase) {
                System.out.println("Caught an AmazonServiceException, which means your request made it "
                        + "to AWS CloudFormation, but was rejected with an error response for some reason.");
                System.out.println("Error Message:    " + ase.getMessage());
                System.out.println("HTTP Status Code: " + ase.getStatusCode());
                System.out.println("AWS Error Code:   " + ase.getErrorCode());
                System.out.println("Error Type:       " + ase.getErrorType());
                System.out.println("Request ID:       " + ase.getRequestId());
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.InternalFailure)
                        .message(ase.getMessage() + " " + ase.getStatusCode() + " " + ase.getErrorCode() + " " + ase.getErrorType() + " " + ase.getRequestId())
                        .build();

            } catch (AmazonClientException ace) {
                System.out.println("Caught an AmazonClientException, which means the client encountered "
                        + "a serious internal problem while trying to communicate with AWS CloudFormation, "
                        + "such as not being able to access the network.");
                System.out.println("Error Message: " + ace.getMessage());
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.InternalFailure)
                        .message(ace.getMessage())
                        .build();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

            AmazonCloudFormation stackbuilder = AmazonCloudFormationClientBuilder.standard()
                    .build();

            //From context check the status of the stack by looking up the stackName property.
            String stackName = callbackContext.getStackName();
            DescribeStacksRequest wait = new DescribeStacksRequest();
            wait.setStackName(stackName);
            String  stackStatus = "Unknown";
            String  stackReason = "";
            List<Stack> stacks = proxy.<DescribeStacksRequest,DescribeStacksResult>injectCredentialsAndInvoke(wait, stackbuilder::describeStacks).getStacks();
            if (
                    stacks.get(0).getStackStatus().equals(StackStatus.CREATE_COMPLETE.toString()) ||
                    stacks.get(0).getStackStatus().equals(StackStatus.CREATE_FAILED.toString()) ||
                    stacks.get(0).getStackStatus().equals(StackStatus.ROLLBACK_FAILED.toString()) ||
                    stacks.get(0).getStackStatus().equals(StackStatus.ROLLBACK_COMPLETE.toString()) ||
                    stacks.get(0).getStackStatus().equals(StackStatus.DELETE_FAILED.toString())
            ) {
                stackStatus = stacks.get(0).getStackStatus();
                stackReason = stacks.get(0).getStackStatusReason();
                String returnString = stackStatus + " (" + stackReason + ")";
                System.out.println(returnString);

                if(stacks.get(0).getStackStatus().equals(StackStatus.CREATE_COMPLETE.toString())) {
                    model.setId(callbackContext.getStackId());
                    model.setOutput(stacks.get(0).getOutputs().get(0).getOutputValue());
                    //DELETE Stack and terminate EC2 instance.
                    AmazonCloudFormation stackBuilder = AmazonCloudFormationClientBuilder.standard()
                            .build();
                    DeleteStackRequest deleteRequest = new DeleteStackRequest();
                    deleteRequest.setStackName(stackName);
                    proxy.injectCredentialsAndInvoke(deleteRequest, stackBuilder::deleteStack);

                    //Make new SSM Parameter with Key=Id and Value=Output
                    AWSSimpleSystemsManagement simpleSystemsManagementClient = ((AWSSimpleSystemsManagementClientBuilder.standard())).build();
                    PutParameterRequest parameterRequest = new PutParameterRequest();
                    parameterRequest.setName(callbackContext.getStackId());
                    parameterRequest.setValue(stacks.get(0).getOutputs().get(0).getOutputValue());
                    parameterRequest.setType("SecureString");
                    if (model.getKeyId() != null || model.getKeyId() != "") {
                        parameterRequest.setKeyId(model.getKeyId());
                    }
                    PutParameterResult parameterResult = proxy.injectCredentialsAndInvoke(parameterRequest, simpleSystemsManagementClient::putParameter);

                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModel(model)
                            .status(OperationStatus.SUCCESS)
                            .build();
                }
                if (stacks.get(0).getStackStatus().equals(StackStatus.CREATE_FAILED.toString()) ||
                    stacks.get(0).getStackStatus().equals(StackStatus.ROLLBACK_COMPLETE)){
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.FAILED)
                            .errorCode(HandlerErrorCode.NotStabilized)
                            .message("Command failed to execute. Please check CloudWatch Logs and the events in the CommandRunner Stack " + stacks.get(0).getStackName())
                            .build();
                }

            } else {
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModel(model)
                            .status(OperationStatus.IN_PROGRESS)
                            .callbackContext(CallbackContext.builder().stackName(stackName).stackId(callbackContext.getStackId()).build())
                            .callbackDelaySeconds(30)
                            .build();
            }

        }

        //It should never reach this code, if it does something went wrong, so it returns internal failure.
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.InternalFailure)
                .message("Internal Failure")
                .build();
    }
}
