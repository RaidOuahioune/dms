from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
import boto3
import os
from botocore.exceptions import ClientError
from typing import Optional, List
import httpx
from jose import jwt
import uuid

app = FastAPI(title="Storage Service", description="File storage service for Document Management System")

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# S3 configuration
AWS_ENDPOINT_URL = os.getenv("AWS_ENDPOINT_URL", "http://localhost:4566")
AWS_ACCESS_KEY_ID = os.getenv("AWS_ACCESS_KEY_ID", "test")
AWS_SECRET_ACCESS_KEY = os.getenv("AWS_SECRET_ACCESS_KEY", "test")
AWS_REGION = os.getenv("AWS_REGION", "us-east-1")
S3_BUCKET_NAME = os.getenv("S3_BUCKET_NAME", "dms-documents")

# Auth service URL
AUTH_SERVICE_URL = os.getenv("AUTH_SERVICE_URL", "http://localhost:8081")

# Initialize S3 client
s3_client = boto3.client(
    's3',
    endpoint_url=AWS_ENDPOINT_URL,
    aws_access_key_id=AWS_ACCESS_KEY_ID,
    aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
    region_name=AWS_REGION
)

# Create bucket if it doesn't exist
def create_bucket_if_not_exists():
    try:
        s3_client.head_bucket(Bucket=S3_BUCKET_NAME)
    except ClientError:
        s3_client.create_bucket(Bucket=S3_BUCKET_NAME)

@app.on_event("startup")
async def startup_event():
    create_bucket_if_not_exists()

# Authentication dependency
async def verify_token(authorization: str = Header(None)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header missing")
    
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization format")
    
    token = authorization.replace("Bearer ", "")
    
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{AUTH_SERVICE_URL}/api/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"}
            )
            
            if response.status_code != 200:
                raise HTTPException(status_code=401, detail="Invalid or expired token")
            
            return response.json()
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Authentication failed: {str(e)}")

# File type validation
ALLOWED_EXTENSIONS = {
    'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 
    'txt', 'csv', 'jpg', 'jpeg', 'png', 'gif'
}

def validate_file_extension(filename: str):
    extension = filename.split('.')[-1].lower() if '.' in filename else ''
    if extension not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400, 
            detail=f"File type not allowed. Allowed types: {', '.join(ALLOWED_EXTENSIONS)}"
        )
    return extension

# File size validation (10MB max)
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB

def validate_file_size(file_size: int):
    if file_size > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"File too large. Maximum size allowed is {MAX_FILE_SIZE / (1024 * 1024)}MB"
        )

# Routes
@app.get("/")
async def root():
    return {"message": "Storage Service is running"}

@app.post("/api/storage/upload")
async def upload_file(
    file: UploadFile = File(...),
    user_info = Depends(verify_token)
):
    # Validate file
    extension = validate_file_extension(file.filename)
    file_content = await file.read()
    validate_file_size(len(file_content))
    
    # Generate unique file key
    file_key = f"{uuid.uuid4()}.{extension}"
    
    # Upload to S3
    try:
        s3_client.put_object(
            Bucket=S3_BUCKET_NAME,
            Key=file_key,
            Body=file_content,
            ContentType=file.content_type
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to upload file: {str(e)}")
    
    return {
        "fileKey": file_key,
        "fileName": file.filename,
        "fileType": file.content_type,
        "fileSize": len(file_content)
    }

@app.get("/api/storage/download-url/{file_key}")
async def get_download_url(
    file_key: str,
    user_info = Depends(verify_token)
):
    try:
        # Generate pre-signed URL valid for 1 hour
        url = s3_client.generate_presigned_url(
            'get_object',
            Params={'Bucket': S3_BUCKET_NAME, 'Key': file_key},
            ExpiresIn=3600
        )
        return url
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate download URL: {str(e)}")

@app.delete("/api/storage/files/{file_key}")
async def delete_file(
    file_key: str,
    user_info = Depends(verify_token)
):
    # Check if user has admin role
    roles = user_info.get("roles", [])
    if "ROLE_ADMIN" not in roles:
        raise HTTPException(status_code=403, detail="Only admins can delete files")
    
    try:
        # Check if file exists
        s3_client.head_object(Bucket=S3_BUCKET_NAME, Key=file_key)
        
        # Delete file
        s3_client.delete_object(Bucket=S3_BUCKET_NAME, Key=file_key)
        return {"message": f"File {file_key} deleted successfully"}
    except ClientError as e:
        if e.response['Error']['Code'] == '404':
            raise HTTPException(status_code=404, detail=f"File {file_key} not found")
        else:
            raise HTTPException(status_code=500, detail=f"Failed to delete file: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
