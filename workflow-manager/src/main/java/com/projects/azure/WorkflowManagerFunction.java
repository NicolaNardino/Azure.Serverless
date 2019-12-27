package com.projects.azure;

import java.util.*;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.projects.azure.event.EventNotification;
import com.projects.azure.event.EventType;
import com.projects.azure.event.EventUtility;

import static com.projects.azure.event.EventUtility.publish;

public class WorkflowManagerFunction {

    private static final Gson gson = new GsonBuilder().create();
    @FunctionName("HttpTrigger-Java")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("name");
        String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        }
    }

    @FunctionName("EventTrigger")
    public HttpResponseMessage eventTrigger(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<Object>> request,
                                   final ExecutionContext context) {
        final Logger logger = context.getLogger();
        logger.info("Event Trigger started.");
        try {
            final Map body = (Map)request.getBody().get();
            final EventNotification eventNotification = new EventNotification(EventType.valueOf(body.get("EventType").toString()), body.get("EventData").toString());
            publish(eventNotification, System.getenv("workflow-manager-eventgrid-topic-key"), System.getenv(System.getenv("workflow-manager-eventgrid-topic-endpoint")));
            logger.info("Published "+eventNotification);
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson(eventNotification)).build();
        }
        catch(final Exception e) {
            final String errorMessage = gson.toJson(e);
            logger.warning(errorMessage);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body(errorMessage).build();
        }
    }
}
