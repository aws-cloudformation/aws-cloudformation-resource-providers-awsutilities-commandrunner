// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.awsutility.cloudformation.commandrunner;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.mockito.Mockito;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.awsutility.cloudformation.commandrunner.CallbackContext;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    //Replace these with your own testing variables.
    private static final String AWS_ACCOUNT_ID = "112233445566";
    private static final String AWS_REGION = "us-east-1";
    private static final String PHYSICAL_ID = "rihgnyajuh";
    private static final String EXPECTED_OUTPUT = "cloudformation-bucket-fgbfgndddd";

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private GetParameterResult parameterResult;

    @Mock
    private Logger logger;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class, Mockito.RETURNS_DEEP_STUBS);
        when(proxy.injectCredentialsAndInvoke(any(GetParameterRequest.class), any(Function.class))).thenReturn(new GetParameterResult().withParameter(new Parameter().withValue(EXPECTED_OUTPUT)));
        logger = mock(Logger.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = ResourceModel.builder().id(PHYSICAL_ID).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .region(AWS_REGION)
            .awsAccountId(AWS_ACCOUNT_ID)
            .build();

        final CallbackContext context = CallbackContext.builder()
                .stabilizationRetriesRemaining(2)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, context, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getOutput()).isEqualTo(EXPECTED_OUTPUT);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
