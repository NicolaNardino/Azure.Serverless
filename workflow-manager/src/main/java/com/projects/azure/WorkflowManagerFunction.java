package com.projects.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import com.microsoft.azure.eventgrid.models.StorageBlobCreatedEventData;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.projects.azure.event.EventNotification;
import com.projects.azure.event.EventType;
import com.projects.azure.market_data.BollingerBandsManager;
import com.projects.azure.market_data.MarketData;
import com.projects.azure.market_data.TradingStrategyInput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.projects.azure.Utility.*;
import static java.time.LocalDate.parse;
import static java.util.stream.Collectors.toList;

public class WorkflowManagerFunction {

    private static final Gson gson = new GsonBuilder().create();
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String okResult = "{Result:\"Ok\"}";

    @FunctionName("EventPublisher")
    public HttpResponseMessage eventPublisher(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<Map<String, String>>> request,
                                   final ExecutionContext context) {
        final Logger logger = context.getLogger();
        try {
            final Map<String, String> body = request.getBody().get();
            final EventNotification eventNotification = new EventNotification(EventType.valueOf(body.get("EventType")), body.get("EventData"));
            publishEvent(eventNotification, System.getenv("workflow-manager-eventgrid-topic-key"), System.getenv("workflow-manager-eventgrid-topic-endpoint"));
            logger.info("Published "+eventNotification);
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson(eventNotification)).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"}").build();
        }
    }

    @FunctionName("DownloadMarketData")
    public HttpResponseMessage downloadMarketData(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION, route="downdloadMarketData/{symbols}") HttpRequestMessage<Optional<Map<String, String>>> request,
                                                  @BindingName("symbols") final String symbols, final ExecutionContext context) {
        final Logger logger = context.getLogger();
        try {
            uploadDummyMarketDataFiles(System.getenv("supportblobstg-connection-string"), System.getenv("supportblobstg-input-data-container-name"), Arrays.asList(symbols.split(";")),
                    parse(request.getQueryParameters().get("startDate"), fmt), logger);
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(okResult).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"}").build();
        }
    }

    @FunctionName("BuildBollingerBands")
    public HttpResponseMessage buildBollingerBands(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<Map<String, String>>> request, final ExecutionContext context) {
        final Logger logger = context.getLogger();
        try {
            final String blobStorageConnectionString = System.getenv("supportblobstg-connection-string");
            final BlobContainerClient inputContainer = getBlobContainerClient(blobStorageConnectionString, System.getenv("supportblobstg-input-data-container-name"));
            final BlobContainerClient outputContainer = getBlobContainerClient(blobStorageConnectionString, System.getenv("supportblobstg-output-data-container-name"));
            inputContainer.listBlobs().forEach(blobItem -> {
                final String blobText = getBlobContent(inputContainer, blobItem.getName(), Charset.defaultCharset());
                final MarketData[] marketDataArray = Arrays.stream(blobText.split(System.lineSeparator())).map(line -> gson.fromJson(line, MarketData.class)).toArray(MarketData[]::new);
                final List<TradingStrategyInput> tradingStrategyInput = BollingerBandsManager.getTradingStrategyInput(marketDataArray, 10);
                uploadFiles(outputContainer, blobItem.getName(), tradingStrategyInput, logger);
            });
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson(okResult)).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"").build();
        }
    }

    @FunctionName("GetBollingerBands")
    public HttpResponseMessage getBollingerBands(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION, route="bollingerBands/{symbol}") HttpRequestMessage<Optional<String>> request,
            @BindingName("symbol") final String symbol, final ExecutionContext context) {
        final Logger logger = context.getLogger();
        try {
            final BlobContainerClient container = getBlobContainerClient(System.getenv("supportblobstg-connection-string"), System.getenv("supportblobstg-output-data-container-name"));
            final String blobText = getBlobContent(container, symbol+".txt", Charset.defaultCharset());
            final List<TradingStrategyInput> tradingStrategyInputs = Arrays.stream(blobText.split(System.lineSeparator())).map(line -> gson.fromJson(line, TradingStrategyInput.class)).collect(toList());
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson(tradingStrategyInputs)).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"}").build();
        }
    }

    @FunctionName("WorkflowErrorManager")
    public HttpResponseMessage workflowErrorManager(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) final HttpRequestMessage<Optional<Map<String, String>>> request,
                                                    final ExecutionContext context) {
        final Logger logger = context.getLogger();
        try {
            final Map<String, String> body = request.getBody().get();
            logger.info("Error source: "+body.get("Source")+"/ message: "+body.get("Message"));
            //TODO: write error to CosmoDB or Storage Account.
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(gson.toJson(okResult)).build();
        }
        catch(final Exception e) {
            logger.warning(printStackTrace(e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json").body("{Error:\""+e.getMessage()+"\"}").build();
        }
    }

    @FunctionName("ProcessedMarketDataAvailableEventGridListener")
    public void processedMarketDataAvailableEventGridListener(@EventGridTrigger(name = "data") final String data, final ExecutionContext context) {
        final Logger logger = context.getLogger();
        try {
            final EventGridEvent eventGridEvent = gson.fromJson(data, EventGridEvent.class);
            logger.info("Event grid event: "+eventGridEvent.data()+"/ "+eventGridEvent.subject()+"/ "+eventGridEvent.eventType());
            final EventNotification eventNotification = gson.fromJson((String) eventGridEvent.data(), EventNotification.class);
            logger.info("Received event: "+eventNotification);
        } catch (Exception e) {
            logger.warning(printStackTrace(e));
        }
    }

    private static void uploadDummyMarketDataFiles(final String blobStorageConnectionString, final String containerName, final List<String> symbols, final LocalDate marketDataStartDate, final Logger logger) {
        final BlobContainerClient containerClient = getBlobContainerClient(blobStorageConnectionString, containerName);
        buildMarketData(symbols.isEmpty()?defaultSymbols:symbols, marketDataStartDate, LocalDate.now()).forEach((k, v) -> uploadFiles(containerClient, k, v, logger));
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