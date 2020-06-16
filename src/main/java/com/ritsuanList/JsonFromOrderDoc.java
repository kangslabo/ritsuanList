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
import com.ritsuanList.logic.TextProcessForOrder;
import opennlp.tools.util.StringUtil;

import java.util.List;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class JsonFromOrderDoc {
    /**
     * This function listens at endpoint "/api/JsonFromOrderDoc". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/JsonFromOrderDoc
     * 2. curl {your host}/api/JsonFromOrderDoc?name=HTTP%20Query
     */
    @FunctionName("JsonFromOrderDoc")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        String pdfFile = request.getBody().orElse("");

        if (StringUtil.isEmpty(pdfFile)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("no exist file contents").build();
        }

        List<String> txtList = new BinaryToFile().destFile(pdfFile, "C:\\\\tmp", "\\\\order.pdf");


        TextProcessForOrder tp = new TextProcessForOrder();
        String object = tp.textToObject(txtList);

        return request.createResponseBuilder(HttpStatus.OK).body(object).build();
    }
}
