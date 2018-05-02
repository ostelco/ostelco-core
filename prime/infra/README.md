# Deploying Prime to Kubernetes

    #PROJECT_ID=pantel-2decb
    export PROJECT_ID="$(gcloud config get-value project -q)"

Reference:
 * https://cloud.google.com/endpoints/docs/grpc/get-started-grpc-kubernetes-engine

## Secrets

    kubectl create secret generic pantel-prod.json --from-file config/pantel-prod.json
    
Reference:
 * https://cloud.google.com/kubernetes-engine/docs/concepts/secret
 * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-files-from-a-pod

## Endpoint

Generate self-contained protobuf descriptor file - api_descriptor.pb

    pip install grpcio grpcio-tools
    
    python -m grpc_tools.protoc \
        --include_imports \
        --include_source_info \
        --proto_path=../ocs-api/src/main/proto \
        --descriptor_set_out=api_descriptor.pb \
        ocs.proto
        
Deploy endpoints

    gcloud endpoints services deploy api_descriptor.pb infra/ocs-api_config.yaml

## Deployment & Service

Build the Docker image (In the folder with Dockerfile)
    
    docker build -t gcr.io/${PROJECT_ID}/prime:1.0.2 .
Push to the registry
    
    gcloud docker -- push gcr.io/${PROJECT_ID}/prime:1.0.2

Apply the deployment & service
    
    kubectl apply -f infra/prime.yaml

Details of the deployment

    kubectl describe deployment prime
    kubectl get pods

Details of service

    kubectl describe service prime-service

