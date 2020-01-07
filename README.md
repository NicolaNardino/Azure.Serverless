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
### IAM
I've managed the access to resources in a mixed way:
- Through directly using resource keys, which is a no-go for productive systems.
- Through Service Principal. That's good, but not ideal, from my point of view.
- Through Managed Identities, both System (MSI) or User generated. 
...

### Development Environment
- Ubuntu 19.04.
- IntelliJ.
- Java 8. (Version 8 due to compatibility with the Azure Functions Java Runtime being capped at that Java version).

#### Required Software
First off, request an [Azure free account](https://azure.microsoft.com/en-us), then install the following:
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli-apt?view=azure-cli-latest).
- [Azure Functions Core Tools](https://github.com/Azure/azure-functions-core-tools).
- [.NET Core](https://docs.microsoft.com/en-us/dotnet/core/install/linux-package-manager-ubuntu-1904), required for building Azure Functions with triggers like EventGrid.

Very useful [link](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-maven-intellij) on how to create an Azure Functions project with IntelliJ from an archetype, package and deploy to an Azure subscription. 
