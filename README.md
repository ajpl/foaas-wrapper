# FOAAS-Wrapper

Fuck Off As A Service ([FOAAS](https://www.foaas.com)) provides a dummy API to practice against. This project creates an API wrapper around that API.

## Start API Server

1. Clone the project
2. Start the project with:
 ```bash
./gradlew run
```
   - The API will be available at https://0.0.0.0:8080

## Endpoints

### Root

- Path: `/`
- Response: 
    - Content-Type: `text/plain`
    - Content: `Hello World!`

### Message
- Path: `/message`
- Requires Authentication:
    - Type: Basic Auth
    - Credentials: user must be the same as pass
      - e.g. `test:test` -> base64 = `dGVzdDp0ZXN0`
- Rate Limited:
    - Request amount: 5
    - Cool down period: 10 seconds
- Response:
    - Content-Type: `application/json`
    - Content: 
```json
{
   "message": "foo",
   "subtitle": "bar"
}
```
## Run tests
1. Clone the project
2. Run tests with:
 ```bash
./gradlew test
```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[Apache License 2.0](https://choosealicense.com/licenses/apache-2.0/)
