# Azure.Serverless

A cloud-native, event-driven application built from Azure serverless services. It uploads market data to Blob Storage and, reacting to events, computes [Bollinger Bands](https://en.wikipedia.org/wiki/Bollinger_Bands) as inputs to a trading strategy. An HTTP API serves the resulting bands.

## Architecture

| Service | Role |
|---|---|
| **Event Grid** | Custom topic and custom events (`MarketDataAvailable`, `TradingStrategyDataAvailable`) for event-based communication, with topic subscriptions delivering events to Function Apps. |
| **Logic App** | Orchestrates the front-to-back workflow. It is triggered by the `MarketDataAvailable` custom event. |
| **Data Factory** | Pipeline of Azure Function activities: market data upload, Bollinger Bands calculation, event notification and error handling. |
| **Azure Functions (Java)** | The serverless code: event publishing, market data upload, band calculation, the bands API and error handling. |
| **Blob Storage** | Input container for raw market data and output container for the calculated bands. |
| **Key Vault** | Holds the storage and Event Grid topic secrets. |

### End-to-end flow

1. A client raises `MarketDataAvailable` by calling the `EventPublisher` function, which publishes it to the Event Grid topic.
2. The event triggers the Logic App, which starts the Data Factory pipeline.
3. The pipeline calls `DownloadMarketData`, which generates daily market data for the requested symbols from a start date and uploads one file per symbol to the input container.
4. It then calls `BuildBollingerBands`, which reads every input file, computes the bands and writes them to the output container.
5. Finally it calls `EventPublisher` to raise `TradingStrategyDataAvailable`, which `TradingStrategyDataAvailableEventGridListener` consumes by appending to an event log blob.
6. Any failing activity reports to `WorkflowErrorManager`.
7. `GetBollingerBands` serves the results per symbol.

### Logic App

![Logic App](https://user-images.githubusercontent.com/8766989/71741597-941e3580-2e5f-11ea-978b-914afda40a4d.png)

### Data Factory

![Data Factory](https://user-images.githubusercontent.com/8766989/71740995-01c96200-2e5e-11ea-96e5-0bd009a4677f.png)

The pipeline's ARM template export is in [`ARM Templates/Data Factory`](ARM%20Templates/Data%20Factory).

### Azure Functions

![Azure Functions](https://user-images.githubusercontent.com/8766989/71741044-24f41180-2e5e-11ea-8d57-b37a2fc7ae71.png)

### Event Grid topic subscriptions

![Event Grid topic subscriptions](https://user-images.githubusercontent.com/8766989/71741129-54a31980-2e5e-11ea-80ed-5e635c9522f8.png)

## Functions

All functions live in [`WorkflowManagerFunction`](workflow-manager/src/main/java/com/projects/azure/WorkflowManagerFunction.java) in the `workflow-manager-function` app. HTTP functions use function-level keys (`?code=...`).

| Function | Trigger | Purpose |
|---|---|---|
| `EventPublisher` | `POST /api/EventPublisher` | Publishes a custom event to the Event Grid topic. Body: `{"EventType": "...", "EventData": "..."}` |
| `DownloadMarketData` | `POST /api/DownloadMarketData` | Generates market data and uploads it to the input container. Body: `{"Symbols": "AAPL;MSFT", "StartDate": "yyyyMMdd"}` |
| `BuildBollingerBands` | `POST /api/BuildBollingerBands` | Computes the bands for every input file and writes them to the output container. |
| `GetBollingerBands` | `GET /api/bollingerBands/{symbol}` | Returns the bands for a symbol as JSON. |
| `WorkflowErrorManager` | `POST /api/WorkflowErrorManager` | Receives workflow errors. Body: `{"Source": "...", "Message": "..."}` |
| `TradingStrategyDataAvailableEventGridListener` | Event Grid | Logs `TradingStrategyDataAvailable` events to a blob. |

### Bollinger Bands

Computed in [`BollingerBandsManager`](workflow-manager/src/main/java/com/projects/azure/market_data/BollingerBandsManager.java) over a rolling window of N closing prices (N = 10 in `BuildBollingerBands`):

- Middle band: N-day simple moving average
- Upper band: middle band + 2 × N-day sample standard deviation
- Lower band: middle band − 2 × N-day sample standard deviation

The first N days of each series serve only as history, so output starts at day N + 1.

## Triggering the workflow

```bash
curl -X POST "https://workflow-manager-function.azurewebsites.net/api/EventPublisher?code=<function-key>" \
  -H "Content-Type: application/json" \
  -d '{"EventType":"MarketDataAvailable", "EventData":"no data"}'
```

Then fetch the bands for a symbol:

```bash
curl "https://workflow-manager-function.azurewebsites.net/api/bollingerBands/AAPL?code=<function-key>"
```

## Identity and access

Access to resources is managed in three ways, deliberately mixed for comparison:

- **Resource keys** used directly. Simple, but not acceptable for production systems.
- **Service principal.** Better, but credentials still have to be managed and rotated.
- **Managed identities**, both system-assigned and user-assigned. The preferred option, since no credentials live in code or configuration.

## Configuration

The function app reads these application settings (typically backed by Key Vault references):

| Setting | Purpose |
|---|---|
| `supportblobstg-connection-string` | Blob Storage connection string |
| `supportblobstg-input-data-container-name` | Container for raw market data |
| `supportblobstg-output-data-container-name` | Container for Bollinger Bands and the event log |
| `workflow-manager-eventgrid-topic-endpoint` | Event Grid custom topic endpoint |
| `workflow-manager-eventgrid-topic-key` | Event Grid custom topic key |

## Build and deploy

### Prerequisites

- An [Azure account](https://azure.microsoft.com/free/)
- [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli)
- [Azure Functions Core Tools](https://github.com/Azure/azure-functions-core-tools)
- [.NET SDK](https://dotnet.microsoft.com/download), required by Core Tools to install binding extensions such as Event Grid
- JDK 8 and Maven 3

### Commands

```bash
az login
cd workflow-manager
mvn clean package                 # build
mvn azure-functions:run           # run locally with Core Tools
mvn azure-functions:deploy        # deploy to the Function App configured in pom.xml
```

Set `functionResourceGroup` and `functionAppRegion` in `workflow-manager/pom.xml` before deploying.

Microsoft's guide to [creating a Java function app with IntelliJ](https://learn.microsoft.com/azure/azure-functions/functions-create-maven-intellij) covers project creation from the Maven archetype, packaging and deployment.

## Project structure

```
pom.xml                       Parent POM (Java 8, Azure Functions Maven plugin)
workflow-manager/             Function app: functions, event model, Bollinger Bands logic
ARM Templates/Data Factory/   Exported Data Factory ARM template
```

## Development environment

- Ubuntu 19.04, IntelliJ IDEA
- Java 8. At the time, Java 8 was the only version supported by the Azure Functions Java runtime.

## Status

This was built in early 2020 as a learning project and is no longer maintained. Market data is generated rather than downloaded, and the hosted endpoints may no longer exist. Azure Functions has since added support for Java 11, 17 and 21, so a revival should start by moving off Java 8 and the 1.x Functions Maven plugin.
