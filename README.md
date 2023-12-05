# QEats
QEats is a popular food ordering app that allows users to browse and order their favorite dishes from nearby restaurants.

During the course of this project,

    I had build different parts of the QEats backend which is a Spring Boot application.
    Several REST API endpoints were implemented to query restaurant information and place food orders.
    To give a sense of real-world problems, production issues were investigated using Scientific Debugging methods.
    Along with this, I improved the app performance under large load scenarios as well as included an advanced search feature in the app. 
Some scrrenshots of the application
![image](https://github.com/sh-arka22/Q-Eats/assets/91637787/48d70af4-05f7-4bac-85fb-939d8e25d319)



## Working in the backend
![image](https://github.com/sh-arka22/Q-Eats/assets/91637787/da5a891b-06b7-42e7-b792-165bb85fdf26)
At a broader level, clients initiate requests to the server. These requests adhere to REST API principles and are sent to the server using the HTTP protocol.

When the request reaches the server, a response is triggered in 3 stages:

    Controller layer: The request is intercepted by a controller that directs it to the service layer.

    Service layer: The specification of the request is understood by the business logic function and sent to the next stage for data retrieval.

    Repository layer: Based on the requirements, appropriate data is retrieved from the database and returned to the service layer

