# Serverless App for downloading books

This repo contains an app for parsing book pages images from libray web-site and assembling them into the one PDF document

## Roadmap
- [ ] Parsing pages URLs by book address;
- [ ] Saving book pages URLs into the DynamoDD table;
- [ ] Chuncking the pages by size 10 and sending them into the SQS;
- [ ] Downloading pages by SQS message and saving them into the S3 bucket;
- [ ] Assembling the images into one PDf document;
- [ ] Cleaning up the page images;
- [ ] Sending an email with a link to the PDf document to the user;
