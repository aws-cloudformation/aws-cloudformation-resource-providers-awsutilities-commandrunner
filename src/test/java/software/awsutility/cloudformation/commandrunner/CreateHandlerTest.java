// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.awsutility.cloudformation.commandrunner;

import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.simplesystemsmanagement.model.*;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Function;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    //Replace these with your own testing variables.
    private static final String COMMAND = "aws s3 cp s3://cfn-cli-project/S3BucketCheck.py . && python S3BucketCheck.py my-bucket cloudformation-bucket-fgbfgndddd";
    private static final String LOG_GROUP = "my-cloudwatch-log-group";
    private static final String ROLE = "my-example-role";
    private static final String AWS_ACCOUNT_ID = "112233445566";
    private static final String AWS_REGION = "us-east-1";

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        when(proxy.injectCredentialsAndInvoke(any(GetParameterRequest.class), any(Function.class))).thenReturn(new GetParameterResult().withParameter(new Parameter().withValue("ami-1234")));

        when(proxy.injectCredentialsAndInvoke(any(DescribeVpcsRequest.class), any(Function.class))).thenReturn(new DescribeVpcsResult().withVpcs(new Vpc().withVpcId("vpc-1234").withIsDefault(Boolean.TRUE)));

        when(proxy.injectCredentialsAndInvoke(any(DescribeSubnetsRequest.class), any(Function.class))).thenReturn(new DescribeSubnetsResult().withSubnets(new Subnet().withSubnetId("subnet-1234")));

        when(proxy.injectCredentialsAndInvoke(any(CreateStackRequest.class), any(Function.class))).thenReturn(new CreateStackResult());

        when(proxy.injectCredentialsAndInvoke(any(DescribeStacksRequest.class), any(Function.class))).thenReturn(new DescribeStacksResult().withStacks(new Stack().withStackStatus(StackStatus.CREATE_COMPLETE).withStackStatusReason("Successful").withOutputs(new Output().withOutputValue("expected-value"))));

        when(proxy.injectCredentialsAndInvoke(any(DeleteStackRequest.class), any(Function.class))).thenReturn(new DeleteStackResult());

        when(proxy.injectCredentialsAndInvoke(any(PutParameterRequest.class), any(Function.class))).thenReturn(new PutParameterResult());

        when(proxy.injectCredentialsAndInvoke(any(GetCallerIdentityRequest.class), any(Function.class))).thenReturn(new GetCallerIdentityResult().withArn("arn:aws:sts::123456789012:assumed-role/my-role-name/my-role-session-name"));

        when(proxy.injectCredentialsAndInvoke(any(SimulatePrincipalPolicyRequest.class), any(Function.class))).thenReturn(new SimulatePrincipalPolicyResult().withEvaluationResults(new EvaluationResult().withEvalDecision("Allowed")));

        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder().command(COMMAND).logGroup(LOG_GROUP).role(ROLE).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .awsAccountId(AWS_ACCOUNT_ID)
            .region(AWS_REGION)
            .build();
        try {
            final ProgressEvent<ResourceModel, CallbackContext> response
                    = handler.handleRequest(proxy, request, null, logger);
            //IN_PROGRESS ASSERTS
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
            assertThat(response.getCallbackContext()).isNotNull();
            assertThat(response.getCallbackDelaySeconds()).isEqualTo(90);
            assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
            assertThat(response.getResourceModels()).isNull();
            assertThat(response.getMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
            request.setDesiredResourceState(request.getDesiredResourceState());
            System.out.println(response.getCallbackContext().getStackName());
            final ProgressEvent<ResourceModel, CallbackContext> nextResponse = handler.handleRequest(proxy, request, response.getCallbackContext(), logger);

            //CREATE_COMPLETE ASSERTS
            assertThat(nextResponse).isNotNull();
            assertThat(nextResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
            assertThat(nextResponse.getCallbackDelaySeconds()).isEqualTo(0);
            assertThat(nextResponse.getCallbackContext()).isNull();
            assertThat(nextResponse.getResourceModel()).isEqualTo(request.getDesiredResourceState());
            assertThat(nextResponse.getResourceModels()).isNull();
            assertThat(nextResponse.getMessage()).isNull();
            assertThat(nextResponse.getErrorCode()).isNull();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
