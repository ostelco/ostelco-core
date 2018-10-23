const fs = require('fs');
const {buildSchema} = require('graphql');

const express = require('express');
const graphqlHTTP = require('express-graphql');
const fetch = require('node-fetch');

const schemaFileContents = fs.readFileSync('schema.graphqls', 'utf8');
const schema = buildSchema(schemaFileContents);

const _ = require('lodash');

const app = express();

const url = 'http://localhost:7474/graphql';

async function graphQLFetcherAysnc(graphQLParams) {

    const queryParams = '?' + Object.keys(graphQLParams).filter(function (key) {
        return Boolean(graphQLParams[key]);
    }).map(function (key) {
        return encodeURIComponent(key) + '=' +
            encodeURIComponent(graphQLParams[key]);
    }).join('&');

    const urlWithParam = new URL(url + queryParams);

    const resp = await fetch(urlWithParam, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
        },
    });
    const json = await resp.json();
    return json.data;
}

app.use(express.static(__dirname));

app.use('/graphql', graphqlHTTP(async (request, response, graphQLParams) => ({
    schema: schema,
    graphiql: true,
    rootValue: await graphQLFetcherAysnc(graphQLParams)
})));

app.listen(4000);

console.log(`Started on http://localhost:4000/`);