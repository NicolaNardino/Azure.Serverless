# Welcone to Azure.Serverless
It's about putting together some of the most interesting Azure Serverless resources to build a cloud-native application that uploads Market Data to Blob storage and then, in a event-based, fashion builds Bollinger Bands as Trading Strategy inputs.
It also provides an API to get the bands.

Main features:
- Event Grid, with a custom Topic and Event, for even-based communication.
- Azure Functions for the Serverless code.  
- Azure Data Factory for the data transformation pipeline, i.e., from row Market Data to Bollinger Bands and event notification.
- Azure Logic App for triggering the front-to-back process and workflow managements. Specifically, it gets triggered by a custom event.
- Azure Key Vault for storing storage and event grid topic secrets.
- Event Grid Topic subscriptions to Function Apps. 



...
