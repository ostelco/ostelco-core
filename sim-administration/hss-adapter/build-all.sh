#!/bin/bash

gradle build && docker build -t hss-adapter . && docker-compose up

