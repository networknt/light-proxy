## Sample Nodejs restful API

### initial environment

Verify if nodejs installed on the local environment or not:

node -v
npm -v

If not, install it first

1. Getting to the node API folder and initial npm:

```
cd nodeapp

npm init
```

2. Install  express & nodemon

```
npm i express

npm i -g nodemon

```

3. Open the package.json file and add this task to the script

```
"start": "nodemon server.js"

```

4. start server:

```
npm run start
```

Nodejs API server start on 8080

### verify from postman

Get books:

```
URL:   http://localhost:8080/api/books/
Method: GET

```

Add book:

```
URL:   http://localhost:8080/api/books/
Method: GET
request body:
{"name":"mybook"}
```

