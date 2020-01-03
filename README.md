# Welcome to Azure.Serverless
It's about putting together some of the most interesting Azure Serverless resources to build a cloud-native application that uploads Market Data to Blob storage and then, in a event-based, fashion builds Bollinger Bands as Trading Strategy inputs.
It also provides an API to get the bands.

Main features:
- Event Grid, with a custom Topic and Event, for even-based communication.
- Azure Functions for the Serverless code.  
- Azure Data Factory for the data transformation pipeline, i.e., from row Market Data to Bollinger Bands and event notification.
- Azure Logic App for triggering the front-to-back process and workflow managements. Specifically, it gets triggered by a custom event.
- Azure Key Vault for storing storage and event grid topic secrets.
- Event Grid Topic subscriptions to Function Apps. 


## Logic App
![image](https://user-images.githubusercontent.com/8766989/71741597-941e3580-2e5f-11ea-978b-914afda40a4d.png)

## Data Factory
![image](https://user-images.githubusercontent.com/8766989/71740995-01c96200-2e5e-11ea-96e5-0bd009a4677f.png)

## Azure Functions
![image](https://user-images.githubusercontent.com/8766989/71741044-24f41180-2e5e-11ea-8d57-b37a2fc7ae71.png)

## Event Grid Topic Subscriptions
![image](https://user-images.githubusercontent.com/8766989/71741129-54a31980-2e5e-11ea-80ed-5e635c9522f8.png)

## Workflow Trigger
The whole workflow gets triggered by raising the event "MarketDataAvailable", technically by calling a RESTful endpoint running on a Azure Function: 
```unix
curl -d '{"EventType":"MarketDataAvailable", "EventData":"no data"}' -H "Content-Type: application/json" -X POST https://workflow-manager-function.azurewebsites.net/api/EventPublisher?code=xxx
```

### Development Environment
- Ubuntu 19.04.
- IntelliJ.
- Java 8. (Version 8 due to compatibility with the Azure Functions Java Runtime being capped at that Java version).
- Azure Core Functions.
- Azure Free Account.
