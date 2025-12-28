# Email Ingestion Service

## Overview

The Email Ingestion Service is a Spring Boot microservice that automatically monitors a configured email account and processes document attachments. When an email with valid attachments is received, the service automatically extracts the documents and injects them into the existing document processing pipeline (MinIO → Database → RabbitMQ → OCR → GenAI).

This service enables users to submit documents simply by sending an email, without needing to use the web interface.

## Architecture

```
Email Account (IMAP/IMAPS)
    ↓
Email Polling (every 30 seconds)
    ↓
Extract Attachments + Validate
    ↓
Upload to MinIO
    ↓
Save Metadata to PostgreSQL
    ↓
Send to RabbitMQ (ocr-worker-queue)
    ↓
OCR Worker → GenAI Worker → Summary Update
```

The email ingestion service **reuses the exact same processing pipeline** as REST uploads, ensuring consistency.

## Features

- **Automatic Email Polling**: Checks configured IMAP/IMAPS account every 30 seconds (configurable)
- **File Validation**: Validates file type and size before processing
- **Sender Tracking**: Uses email sender's address as the document author
- **Duplicate Detection**: Prevents duplicate files from the same author
- **Shared Infrastructure**: Uses the same MinIO, PostgreSQL, and RabbitMQ as the REST service
- **Processing Pipeline Integration**: Documents processed identically to web uploads (OCR + GenAI summary)
- **Health Checks**: Docker health monitoring via Spring Actuator
- **Flexible Configuration**: Support for Gmail, Outlook, or any IMAP-compatible email service

## Setup Instructions

### 1. Configure Email Account

#### For Gmail:

1. **Enable 2-Factor Authentication** on your Google account
2. **Generate App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and "Other (Custom name)"
   - Name it "Document Management System"
   - Copy the generated 16-character password

3. **Update `.env` file**:
```bash
EMAIL_HOST=imap.gmail.com
EMAIL_PORT=993
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=abcd-efgh-ijkl-mnop  # Your 16-character app password
EMAIL_PROTOCOL=imaps
EMAIL_POLLING_INTERVAL=30000
EMAIL_POLLING_ENABLED=true
```

#### For Outlook/Office365:

```bash
EMAIL_HOST=outlook.office365.com
EMAIL_PORT=993
EMAIL_USERNAME=your-email@outlook.com
EMAIL_PASSWORD=your-password
EMAIL_PROTOCOL=imaps
```

#### For Custom IMAP Server:

```bash
EMAIL_HOST=mail.yourdomain.com
EMAIL_PORT=993
EMAIL_USERNAME=documents@yourdomain.com
EMAIL_PASSWORD=your-password
EMAIL_PROTOCOL=imaps
```

### 2. Build and Deploy

#### Using Docker Compose (Recommended):

```bash
# Build the email-ingestion service
docker-compose build email-ingestion

# Start all services including email-ingestion
docker-compose up -d

# View logs
docker-compose logs -f email-ingestion
```

#### Standalone Build:

```bash
cd email-ingestion
mvn clean package
java -jar target/email-ingestion-0.0.1-SNAPSHOT.jar
```

### 3. Verify Service is Running

```bash
# Check health endpoint
curl http://localhost:8082/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"},
    "rabbit": {"status": "UP"}
  }
}
```

## Usage

### Sending Documents via Email

1. **Compose an email** to the configured email address
2. **Attach documents** (PDF, Word, Excel, images, etc.)
3. **Send the email**
4. **Wait 30 seconds** for the polling cycle
5. **Check the web UI** - the document should appear in the document list

### Example Email:

```
To: your-configured-email@gmail.com
Subject: Invoice Q4 2024
Attachments: invoice.pdf, receipt.png

Body: (optional - not processed)
```

### What Happens:

1. Email ingestion service polls the inbox every 30 seconds
2. Detects new unread email with attachments
3. Extracts `invoice.pdf` and `receipt.png`
4. Validates file types and sizes
5. Uploads both files to MinIO
6. Saves metadata to database (author = sender's email)
7. Sends messages to RabbitMQ OCR queue
8. OCR worker extracts text
9. GenAI worker generates summaries
10. Summaries appear in web UI

## Configuration Reference

### Email Settings

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_HOST` | `imap.gmail.com` | IMAP server hostname |
| `EMAIL_PORT` | `993` | IMAP server port (993 for IMAPS, 143 for IMAP) |
| `EMAIL_USERNAME` | - | Email account username |
| `EMAIL_PASSWORD` | - | Email account password (use app password for Gmail) |
| `EMAIL_PROTOCOL` | `imaps` | Protocol: `imaps` (SSL) or `imap` |
| `EMAIL_POLLING_INTERVAL` | `30000` | Polling interval in milliseconds (30000 = 30 seconds) |
| `EMAIL_POLLING_ENABLED` | `true` | Enable/disable email polling |
| `EMAIL_FOLDER` | `INBOX` | Email folder to monitor |
| `EMAIL_MARK_AS_READ` | `true` | Mark processed emails as read |
| `EMAIL_DELETE_AFTER_PROCESSING` | `false` | Delete emails after processing (not recommended) |

### File Validation Settings

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_ALLOWED_EXTENSIONS` | `pdf,doc,docx,txt,png,jpg,jpeg,gif,xls,xlsx,ppt,pptx,csv,zip` | Allowed file extensions (comma-separated) |
| `EMAIL_MAX_FILE_SIZE` | `52428800` | Maximum file size in bytes (50 MB) |

### Shared Infrastructure Settings

These settings are shared with the REST service and should match:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/organization` | PostgreSQL database URL |
| `RABBITMQ_HOST` | `rabbitmq` | RabbitMQ hostname |
| `MINIO_ENDPOINT` | `minio` | MinIO endpoint |
| `MINIO_BUCKET_NAME` | `documents` | MinIO bucket name |

## Monitoring and Troubleshooting

### View Logs

```bash
# Docker logs
docker-compose logs -f email-ingestion

# Last 100 lines
docker-compose logs --tail=100 email-ingestion
```

### Common Log Messages

**Successful Processing:**
```
Email received for processing
Email from: sender@example.com
Subject: My Document
Processing attachment: document.pdf
File uploaded successfully to MinIO
✓ Successfully processed attachment 'document.pdf' from 'sender@example.com'
```

**No Attachments:**
```
No attachments found in email from sender@example.com
```

**Invalid File Type:**
```
Rejected attachment 'malware.exe' - invalid file extension
```

**File Too Large:**
```
Rejected attachment 'large-video.mp4' - file size exceeds limit
```

**Duplicate File:**
```
Duplicate file detected: document.pdf from author: sender@example.com - skipping
```

### Troubleshooting

#### Service Won't Start

**Check credentials:**
```bash
docker-compose logs email-ingestion | grep "Failed to initialize"
```

**Verify email credentials** in `.env` file

#### No Emails Being Processed

**Check polling is enabled:**
```bash
docker-compose logs email-ingestion | grep "Email polling is DISABLED"
```

**Test email connection manually:**
```bash
# Connect to container
docker-compose exec email-ingestion sh

# Test telnet to email server
telnet imap.gmail.com 993
```

**Verify inbox has unread emails** with attachments

#### Attachments Not Appearing in UI

**Check if files were uploaded to MinIO:**
```bash
# Access MinIO console
http://localhost:9090
# Login: admin / admin-password
# Check "documents" bucket
```

**Check database for metadata:**
```bash
docker-compose exec db psql -U postgres -d organization -c "SELECT * FROM file_metadata ORDER BY upload_time DESC LIMIT 10;"
```

**Check RabbitMQ queues:**
```bash
# Access RabbitMQ management UI
http://localhost:15672
# Login: guest / guest
# Check "ocr-worker-queue" has messages
```

## Security Considerations

### Email Account Security

- **Use App Passwords** (Gmail) instead of account passwords
- **Create dedicated email account** for document ingestion
- **Enable 2FA** on the email account
- **Rotate passwords regularly**
- **Never commit `.env`** file to version control

### File Validation

- **Whitelist file extensions** - only allow expected document types
- **Enforce file size limits** - prevent storage abuse
- **Log all processing attempts** - for audit trail
- **Validate sender domains** (optional enhancement)

### Network Security

- **Use IMAPS (SSL/TLS)** for encrypted email retrieval
- **Firewall rules** to restrict email ingestion service access
- **Monitor for anomalies** in processing patterns

## Performance Tuning

### Polling Interval

- **Default: 30 seconds** - good balance between responsiveness and load
- **High volume**: Increase to 60-120 seconds to reduce email server load
- **Low latency**: Decrease to 10-15 seconds for faster processing

```bash
EMAIL_POLLING_INTERVAL=60000  # 60 seconds
```

### Email Folder Strategy

- **Use dedicated folder**: Create "Documents" folder instead of monitoring INBOX
- **Reduces false positives**: Only emails in specific folder are processed
- **Better organization**: Separate personal emails from document submissions

```bash
EMAIL_FOLDER=Documents
```

### File Size Limits

- **Default: 50 MB** - suitable for most documents
- **Adjust based on use case**: Larger for videos, smaller for text documents

```bash
EMAIL_MAX_FILE_SIZE=104857600  # 100 MB
```

## Development

### Project Structure

```
email-ingestion/
├── src/main/java/org/emailingestion/
│   ├── EmailIngestionApplication.java    # Main application class
│   ├── config/
│   │   ├── EmailConfig.java             # Spring Integration email adapter
│   │   ├── MinIOConfig.java             # MinIO client configuration
│   │   └── RabbitMqConfig.java          # RabbitMQ configuration
│   ├── service/
│   │   ├── EmailPollingService.java     # Email message handler
│   │   ├── AttachmentProcessor.java     # Core attachment processing logic
│   │   ├── FileMetadataService.java     # Database + RabbitMQ orchestration
│   │   ├── MinIOFileStorage.java        # MinIO file operations
│   │   └── MessageProducerService.java  # RabbitMQ message producer
│   ├── model/
│   │   └── FileMetadata.java            # JPA entity (shared with REST)
│   ├── repository/
│   │   └── FileMetadataRepository.java  # JPA repository
│   └── dto/
│       └── FileMessageDto.java          # RabbitMQ message format
└── src/main/resources/
    └── application.properties           # Configuration
```

### Key Design Decisions

1. **Code Reuse via Copying**: Shared classes copied from REST module with package renaming
2. **Same Database Table**: `file_metadata` table shared between REST and email-ingestion
3. **Same RabbitMQ Queues**: Both services send to `ocr-worker-queue`
4. **Same MinIO Bucket**: Both services use `documents` bucket
5. **Identical Processing Logic**: `FileMetadataService.createFileMetadataWithWorkerNotification()` method is the same

### Running Tests

```bash
cd email-ingestion
mvn test
```

### Building Locally

```bash
cd email-ingestion
mvn clean package
java -jar target/email-ingestion-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Health Check

```bash
GET http://localhost:8082/actuator/health
```

### Metrics

```bash
GET http://localhost:8082/actuator/metrics
```

### Info

```bash
GET http://localhost:8082/actuator/info
```

## Integration with Existing Services

### REST Service (Port 8081)

- **Shares database**: Both write to `file_metadata` table
- **Shares MinIO**: Both upload to `documents` bucket
- **Shares RabbitMQ**: Both send to `ocr-worker-queue`
- **Independent operation**: Can run without REST service (if database/MinIO/RabbitMQ are available)

### Worker Services

- **No changes required**: Workers consume from same queues
- **Transparent processing**: Workers don't know if document came from REST or email
- **Same output**: GenAI summaries written back to database

### Web UI (Port 8080)

- **Displays all documents**: Email-ingested documents appear in document list
- **Author field**: Shows email sender address
- **Full functionality**: Download, delete, search work the same

## Future Enhancements

- **Email sender whitelist**: Only accept emails from approved senders/domains
- **Email subject parsing**: Extract metadata from email subject line
- **Reply with confirmation**: Send email reply when processing completes
- **Error notifications**: Email sender if attachment processing fails
- **Attachment compression**: Automatically compress large files before storage
- **Virus scanning**: Integrate ClamAV or similar for attachment scanning
- **Multi-folder support**: Monitor multiple email folders simultaneously
- **OAuth2 authentication**: Support modern email authentication methods

## License

This service is part of the Document Management System university project.

## Support

For issues or questions, please check:
- Docker logs: `docker-compose logs email-ingestion`
- Health endpoint: http://localhost:8082/actuator/health
- Email server connectivity
- Configuration in `.env` file
