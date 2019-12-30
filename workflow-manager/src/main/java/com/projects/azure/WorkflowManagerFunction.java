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
import com.projects.azure.market_data.BollingerBandsManager;
import com.projects.azure.market_data.MarketData;
import com.projects.azure.market_data.TradingStrategyInput;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.projects.azure.Utility.*;
import static com.projects.azure.event.EventUtility.publish;
import static java.util.stream.Collectors.toList;

public class WorkflowManagerFunction {

    private static final Gson gson = new GsonBuilder().create();

    @FunctionName("EventPublisher")
    public HttpResponseMessage eventPublisher(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<Object>> request,
                                   final ExecutionContext context) {
        final Logger logger = context.getLogger();
        logger.info("EventPublisher started.");
        try {
            final Map body = (Map)request.getBody().get();
            if (body.get("UploadMarketDataFiles").toString().equalsIgnoreCase("yes"))
                uploadMarketDataFiles(System.getenv("supportblobstg-connection-string"), System.getenv("supportblobstg-input-data-container-name"), logger);
            final EventNotification eventNotification = new EventNotification(EventType.valueOf(body.get("EventType").toString()), body.get("EventData").toString());
            publish(eventNotification, System.getenv("workflow-manager-eventgrid-topic-key"), System.getenv("workflow-manager-eventgrid-topic-endpoint"));
            logger.info("Published "+eventNotification);
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson(eventNotification)).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"}").build();
        }
    }

    @FunctionName("BuildBollingerBands")
    public HttpResponseMessage buildBollingerBands(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<Object>> request,
                                                    final ExecutionContext context) {
        final Logger logger = context.getLogger();
        logger.info("BuildBollingerBands started.");
        try {
            final Map body = (Map)request.getBody().get();
            final String blobStorageConnectionString = System.getenv("supportblobstg-connection-string");
            final BlobContainerClient inputContainerClient = getBlobContainerClient(blobStorageConnectionString, System.getenv("supportblobstg-input-data-container-name"));
            final BlobContainerClient outputContainerClient = getBlobContainerClient(blobStorageConnectionString, System.getenv("supportblobstg-output-data-container-name"));
            inputContainerClient.listBlobs().forEach(blobItem -> {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                inputContainerClient.getBlobClient(blobItem.getName()).download(os);
                final String blobText = new String(os.toByteArray(), Charset.defaultCharset());
                final MarketData[] marketDataArray = Arrays.stream(blobText.split(System.lineSeparator())).map(line -> gson.fromJson(line, MarketData.class)).toArray(MarketData[]::new);
                final List<TradingStrategyInput> tradingStrategyInput = BollingerBandsManager.getTradingStrategyInput(marketDataArray, 10);
                uploadFiles(outputContainerClient, blobItem.getName(), tradingStrategyInput, logger);
            });
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson("{Result:\"Ok\"}")).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"").build();
        }
    }

    private static void uploadMarketDataFiles(final String blobStorageConnectionString, final String containerName, final Logger logger) {
        final BlobContainerClient containerClient = getBlobContainerClient(blobStorageConnectionString, containerName);
        buildMarketData(tickers, LocalDate.of(2019, 1, 1), LocalDate.now()).
                forEach((k, v) -> uploadFiles(containerClient, k, v, logger));
    }

    private static <T> void uploadFiles(final BlobContainerClient containerClient, final String blobFileName, final List<T> v, final Logger logger) {
        try {
            final Path tempFile = Files.createTempFile(blobFileName, ".txt");
            Files.write(tempFile, v.stream().map(gson::toJson).collect(toList()), Charset.defaultCharset());
            final BlobClient blobClient = containerClient.getBlobClient(blobFileName+".txt");
            blobClient.uploadFromFile(tempFile.toAbsolutePath().toString(), true);
            Files.deleteIfExists(tempFile);
        } catch (final IOException e) {
            logger.warning(printStackTrace(e));
            throw new UncheckedIOException(e);
        }
    }

    private static BlobContainerClient getBlobContainerClient(final String blobStorageConnectionString, final String containerName) {
        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(blobStorageConnectionString).buildClient();
        final BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists())
            containerClient.create();
        return containerClient;
    }
}