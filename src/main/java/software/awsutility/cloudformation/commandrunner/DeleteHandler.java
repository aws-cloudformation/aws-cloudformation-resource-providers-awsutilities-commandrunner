// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.awsutility.cloudformation.commandrunner;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.DeleteParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.DeleteParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import software.amazon.cloudformation.proxy.*;
import software.awsutility.cloudformation.commandrunner.CallbackContext;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        AWSSimpleSystemsManagement simpleSystemsManagementClient = ((AWSSimpleSystemsManagementClientBuilder.standard())).build();
        DeleteParameterRequest parameterRequest = new DeleteParameterRequest();
        parameterRequest.setName(model.getId());
        try {

            DeleteParameterResult parameterResult = proxy.injectCredentialsAndInvoke(parameterRequest, simpleSystemsManagementClient::deleteParameter);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .resourceModel(model)
                    .build();
        } catch (Exception e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .errorCode(HandlerErrorCode.NotFound)
                    .status(OperationStatus.FAILED)
                    .build();
        }



    }
}
