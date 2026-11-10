# Seattendance

Seattendance is a Java desktop attendance-management application built with Maven and SQLite. The source includes classroom and student management, attendance reporting, RFID-related components, and email reports.

## Requirements

- Java Development Kit (JDK) 21
- Apache Maven
- For optional RFID scanning, compatible reader hardware and serial-port access

## Build and run

From the `seatendance` subfolder:

```bash
mvn clean test
mvn exec:java
```

The main class configured for Maven Exec is `com.seatendance.App`. The application may need additional local setup or hardware to use all features.

## Email configuration

Email reports are optional. Set these environment variables locally before starting the app:

- `SEATTENDANCE_SMTP_EMAIL`: sender's Gmail account
- `SEATTENDANCE_SMTP_PASSWORD`: a newly generated Gmail app password (where supported)

**Never commit passwords, `.env` files, or real attendance data.** If an older version contained an email password, revoke it before publishing. A secret remains exposed in old Git history even after it is removed from the latest commit.

## Initial administrator setup

For a fresh database, configure these variables locally before launching:

- `SEATTENDANCE_ADMIN_EMAIL`: initial administrator email address
- `SEATTENDANCE_ADMIN_PASSWORD`: strong, unique initial administrator password

The app skips automatic admin creation if these are not configured. Do not publish the password or use the former default login.

## Local data

The application uses SQLite. Local `.db` files are deliberately excluded from this public source package because they may contain private student or attendance information. The startup database schema and initial data requirements may need to be configured for your environment.

## Contributing

Please open an issue describing the bug or proposed change before submitting significant modifications. When opening a pull request, include the steps used to test it.

## License

No license is granted by this package. The repository owner and all relevant rights holders should choose and add an open-source license before describing this project as open source.
