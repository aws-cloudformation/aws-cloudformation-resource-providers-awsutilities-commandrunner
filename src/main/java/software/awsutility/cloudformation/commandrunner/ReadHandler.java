// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.awsutility.cloudformation.commandrunner;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterResult;
import software.amazon.cloudformation.proxy.*;
import software.awsutility.cloudformation.commandrunner.CallbackContext;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        AWSSimpleSystemsManagement simpleSystemsManagementClient = ((AWSSimpleSystemsManagementClientBuilder.standard())).build();
        GetParameterRequest parameterRequest = new GetParameterRequest();
        parameterRequest.setName(model.getId());
        parameterRequest.setWithDecryption(true);
        try {

            GetParameterResult parameterResult = proxy.injectCredentialsAndInvoke(parameterRequest, simpleSystemsManagementClient::getParameter);
            model.setOutput(parameterResult.getParameter().getValue());
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .message(model.getPrimaryIdentifier().toString())
                    .build();
        } catch (Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .errorCode(HandlerErrorCode.NotFound)
                    .status(OperationStatus.FAILED)
                    .build();
        }


    }
}
