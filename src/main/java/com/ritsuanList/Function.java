package com.ritsuanList;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.ritsuanList.logic.BinaryToFile;
import com.ritsuanList.logic.TextProcess;
import opennlp.tools.util.StringUtil;

import java.util.List;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/jsonFromPdf". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/jsonFromPdf&code={your function key}
     * 2. curl "{your host}/api/jsonFromPdf?name=HTTP%20Query&code={your function key}"
     * Function Key is not needed when running locally, it is used to invoke function deployed to Azure.
     * More details: https://aka.ms/functions_authorization_keys
     */
    @FunctionName("jsonFromPdf")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request, final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        String pdfFile = request.getBody().orElse("");

        if (StringUtil.isEmpty(pdfFile)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("no exist file contents").build();
        }

        List<String> txtList = new BinaryToFile().destFile(pdfFile, "C:\\\\tmp", "\\\\ritsuan.pdf");


        TextProcess tp = new TextProcess();
        String ritsuanObject = tp.textToObject(txtList);

        return request.createResponseBuilder(HttpStatus.OK).body(ritsuanObject).build();
    }
}
