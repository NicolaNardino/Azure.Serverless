package com.projects.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.projects.azure.event.EventNotification;
import com.projects.azure.event.EventType;
import com.projects.azure.market_data.MarketData;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.projects.azure.Utility.printStackTrace;
import static com.projects.azure.Utility.tickers;
import static com.projects.azure.event.EventUtility.publish;
import static java.util.stream.Collectors.toList;

public class WorkflowManagerFunction {

    private static final Gson gson = new GsonBuilder().create();

    @FunctionName("DataAvailableTrigger")
    public HttpResponseMessage dataAvailableTrigger(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<Object>> request,
                                   final ExecutionContext context) {
        final Logger logger = context.getLogger();
        logger.info("DataAvailableTrigger started.");
        try {
            final Map body = (Map)request.getBody().get();
            uploadFiles(System.getenv("supportblobstg-connection-string"), System.getenv("supportblobstg-input-data-container-name"), logger);
            final EventNotification eventNotification = new EventNotification(EventType.valueOf(body.get("EventType").toString()), body.get("EventData").toString());
            publish(eventNotification, System.getenv("workflow-manager-eventgrid-topic-key"), System.getenv("workflow-manager-eventgrid-topic-endpoint"));
            logger.info("Published "+eventNotification);
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson(eventNotification)).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"").build();
        }
    }

    private static void uploadFiles(final String connectStr, final String containerName, final Logger logger) {
        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectStr).buildClient();
        final BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists())
            containerClient.create();
        Utility.buildMarketData(tickers, LocalDate.of(2019, 1, 1), LocalDate.now()).
                forEach((k, v) -> {
                    try {
                        final Path tempFile = Files.createTempFile(k, ".csv");
                        Files.write(tempFile, v.stream().map(md -> MarketData.toCSV(md, ";")).collect(toList()), Charset.defaultCharset());
                        final BlobClient blobClient = containerClient.getBlobClient(k+".csv");
                        blobClient.uploadFromFile(tempFile.toAbsolutePath().toString(), true);
                    } catch (final IOException e) {
                        logger.warning(printStackTrace(e));
                        throw new UncheckedIOException(e);
                    }
                });
    }
}
