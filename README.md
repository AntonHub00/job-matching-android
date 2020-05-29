# Job Matching REST API fetching

## Setup

The only configuration needed in this project to make it work (besides the Job
Matching REST API project running in your LAN) is to change the base URL to the
URL of the machine running the REST API in your LAN and recompile the app.

Line 45 of MainActivity.java file:

``` java
private final String base_url = "http://192.168.100.65:8000/";
```
