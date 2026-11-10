# Security guidance

- Never publish live student records or local SQLite databases.
- Configure administrator credentials via `SEATTENDANCE_ADMIN_EMAIL` and `SEATTENDANCE_ADMIN_PASSWORD`.
- Configure SMTP credentials through the `SEATTENDANCE_SMTP_EMAIL` and `SEATTENDANCE_SMTP_PASSWORD` environment variables.
- Revoke any passwords published in earlier project files or repository history. Editing files in a later commit does not revoke them.
- Audit repository history before publishing or changing visibility.
- Report vulnerabilities privately to a maintainer.
