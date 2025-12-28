# Email Ingestion Setup Guide

## Quick Start

This guide will help you set up the Email Ingestion Service to automatically process document attachments sent via email.

---

## Step 1: Configure Your Email Account

### Option A: Gmail (Recommended for Testing)

1. **Create or use existing Gmail account**
   - Example: `documents.dms@gmail.com`

2. **Enable 2-Factor Authentication**
   - Go to: https://myaccount.google.com/security
   - Under "Signing in to Google", enable "2-Step Verification"

3. **Generate App Password**
   - Go to: https://myaccount.google.com/apppasswords
   - Select: App = "Mail", Device = "Other (Custom name)"
   - Enter name: "Document Management System"
   - Click "Generate"
   - **Copy the 16-character password** (e.g., `abcd efgh ijkl mnop`)

### Option B: Outlook/Office365

1. Use your Outlook.com or Office365 email
2. Password is your regular account password
3. May need to enable "Less secure app access" in account settings

### Option C: Custom IMAP Server

1. Contact your email administrator for IMAP settings
2. Get: hostname, port, username, password

---

## Step 2: Update Configuration

Edit the `.env` file in the project root:

```bash
# For Gmail
EMAIL_HOST=imap.gmail.com
EMAIL_PORT=993
EMAIL_USERNAME=documents.dms@gmail.com
EMAIL_PASSWORD=abcd efgh ijkl mnop  # Replace with your app password (remove spaces)
EMAIL_PROTOCOL=imaps
EMAIL_POLLING_INTERVAL=30000
EMAIL_POLLING_ENABLED=true

# For Outlook
EMAIL_HOST=outlook.office365.com
EMAIL_PORT=993
EMAIL_USERNAME=your-email@outlook.com
EMAIL_PASSWORD=your-password
EMAIL_PROTOCOL=imaps
EMAIL_POLLING_INTERVAL=30000
EMAIL_POLLING_ENABLED=true
```

**Important**:
- Remove spaces from Gmail app password: `abcdefghijklmnop`
- Never commit the `.env` file to Git (it's in `.gitignore`)

---

## Step 3: Build and Start the Service

### Using Docker Compose (Recommended)

```bash
# Build the email-ingestion service
docker-compose build email-ingestion

# Start all services including email-ingestion
docker-compose up -d

# Verify email-ingestion is running
docker-compose ps

# Check logs
docker-compose logs -f email-ingestion
```

You should see:
```
========================================
Email Ingestion Service Started
========================================
The service is now monitoring the configured email account for new documents.
```

### Health Check

```bash
curl http://localhost:8082/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "rabbit": {"status": "UP"}
  }
}
```

---

## Step 4: Test the Service

### Send a Test Email

1. **From any email client**, send an email to your configured address
   - **To**: `documents.dms@gmail.com` (or your configured email)
   - **Subject**: `Test Document`
   - **Attachment**: Any PDF, Word doc, or image file
   - **Body**: (optional, will be ignored)

2. **Wait 30 seconds** (default polling interval)

3. **Check the logs**:
   ```bash
   docker-compose logs email-ingestion
   ```

   You should see:
   ```
   Email received for processing
   Email from: your-sender-email@gmail.com
   Subject: Test Document
   Processing attachment: test.pdf
   File uploaded successfully to MinIO
   ✓ Successfully processed attachment 'test.pdf'
   ```

4. **Check the Web UI**:
   - Open: http://localhost:8080
   - Your document should appear in the list
   - **Author**: Will be the sender's email address
   - **Summary**: Will be generated after OCR + GenAI processing (may take 1-2 minutes)

---

## Step 5: Verify End-to-End Processing

### Check MinIO (Object Storage)

1. Open MinIO console: http://localhost:9090
2. Login: `admin` / `admin-password`
3. Navigate to `documents` bucket
4. You should see your file: `{timestamp}-test.pdf`

### Check Database

```bash
docker-compose exec db psql -U postgres -d organization -c "SELECT id, filename, author, file_type, upload_time FROM file_metadata ORDER BY upload_time DESC LIMIT 5;"
```

You should see your file with sender's email as author.

### Check RabbitMQ

1. Open RabbitMQ management: http://localhost:15672
2. Login: `guest` / `guest`
3. Go to "Queues" tab
4. Check `ocr-worker-queue` - should show messages being processed

### Check Summary Generation

Wait 1-2 minutes, then refresh the web UI. The document should now have an AI-generated summary.

---

## Configuration Options

### Change Polling Interval

Edit `.env`:
```bash
# Check every 60 seconds instead of 30
EMAIL_POLLING_INTERVAL=60000
```

Restart the service:
```bash
docker-compose restart email-ingestion
```

### Restrict File Types

Edit `.env`:
```bash
# Only allow PDF and Word documents
EMAIL_ALLOWED_EXTENSIONS=pdf,doc,docx
```

### Increase File Size Limit

Edit `.env`:
```bash
# Allow up to 100 MB files (default is 50 MB)
EMAIL_MAX_FILE_SIZE=104857600
```

### Disable Email Ingestion (Temporarily)

Edit `.env`:
```bash
EMAIL_POLLING_ENABLED=false
```

Restart:
```bash
docker-compose restart email-ingestion
```

---

## Troubleshooting

### Email Service Won't Start

**Error**: `Failed to initialize MinIO client`
- **Solution**: Check that MinIO, PostgreSQL, and RabbitMQ are running:
  ```bash
  docker-compose ps
  ```

**Error**: `Authentication failed`
- **Solution**: Double-check email credentials in `.env`
- For Gmail: Make sure you're using App Password, not regular password
- Remove spaces from app password

### No Emails Being Processed

**Check if polling is enabled**:
```bash
docker-compose logs email-ingestion | grep "Email polling is DISABLED"
```

**Check for connection errors**:
```bash
docker-compose logs email-ingestion | grep "error"
```

**Verify inbox has unread emails** with attachments

### Attachments Rejected

**Invalid file type**:
```
Rejected attachment 'document.exe' - invalid file extension
```
- **Solution**: Only send allowed file types (PDF, DOC, DOCX, TXT, images)

**File too large**:
```
Rejected attachment 'large-file.pdf' - file size exceeds limit
```
- **Solution**: Increase `EMAIL_MAX_FILE_SIZE` in `.env`

### Duplicate Files

```
Duplicate file detected: document.pdf from author: sender@example.com - skipping
```

This is **expected behavior**. The system prevents duplicate files with the same name from the same author. To process again:
- Rename the file before sending
- Or delete the existing file from the web UI first

---

## Architecture Overview

```
┌─────────────────┐
│  Email Client   │
│  (Gmail, etc.)  │
└────────┬────────┘
         │ Send email with attachment
         ▼
┌─────────────────┐
│  IMAP Server    │
│  (imap.gmail)   │
└────────┬────────┘
         │ Poll every 30s
         ▼
┌─────────────────────────────────┐
│  Email Ingestion Service        │
│  (Port 8082)                    │
│  - Extract attachments          │
│  - Validate file type/size      │
│  - Upload to MinIO              │
│  - Save to PostgreSQL           │
│  - Send to RabbitMQ             │
└────────┬────────────────────────┘
         │
         ├─────────────────────┐
         │                     │
         ▼                     ▼
┌─────────────┐       ┌─────────────┐
│   MinIO     │       │ PostgreSQL  │
│  (Storage)  │       │ (Metadata)  │
└─────────────┘       └─────────────┘
         │
         ▼
┌─────────────────┐
│   RabbitMQ      │
│ (Message Queue) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  OCR Worker     │
│ (Extract Text)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  GenAI Worker   │
│ (AI Summary)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Web UI         │
│  (Port 8080)    │
│  Display docs   │
└─────────────────┘
```

---

## Usage Examples

### Example 1: Upload Invoice

**Email:**
- To: `documents.dms@gmail.com`
- Subject: `Q4 Invoice`
- Attachment: `invoice-2024-Q4.pdf`

**Result:**
- Document appears in UI with filename: `invoice-2024-Q4.pdf`
- Author: Your email address
- AI Summary: "This is an invoice for Q4 2024..."

### Example 2: Upload Multiple Files

**Email:**
- To: `documents.dms@gmail.com`
- Subject: `Project Files`
- Attachments: `report.pdf`, `data.xlsx`, `diagram.png`

**Result:**
- All 3 files processed separately
- Each appears as individual document in UI
- Each gets its own AI summary

### Example 3: Invalid File Type

**Email:**
- To: `documents.dms@gmail.com`
- Subject: `Program`
- Attachment: `malware.exe`

**Result:**
- File rejected (logged but not processed)
- Check logs: `Rejected attachment 'malware.exe' - invalid file extension`

---

## Security Best Practices

1. **Use dedicated email account** for document ingestion (not your personal email)
2. **Enable 2FA** on the email account
3. **Use App Passwords** (Gmail) instead of regular passwords
4. **Restrict file types** to only necessary formats
5. **Monitor logs** for suspicious activity
6. **Rotate passwords** regularly
7. **Never commit `.env`** file to Git

---

## Next Steps

1. ✅ Configure email account
2. ✅ Update `.env` file
3. ✅ Start services with `docker-compose up -d`
4. ✅ Send test email
5. ✅ Verify document appears in web UI
6. ✅ Check AI summary generation

**You're all set!** Users can now submit documents by simply sending an email with attachments.

---

## Additional Resources

- **Full Documentation**: See `email-ingestion/README.md`
- **Docker Compose**: `docker-compose.yml`
- **Configuration**: `.env` file
- **Logs**: `docker-compose logs -f email-ingestion`
- **Health Check**: http://localhost:8082/actuator/health
- **Web UI**: http://localhost:8080

---

## Support

If you encounter issues:

1. Check logs: `docker-compose logs email-ingestion`
2. Verify all services are running: `docker-compose ps`
3. Test email credentials manually
4. Check `.env` configuration
5. Review email-ingestion/README.md for detailed troubleshooting
