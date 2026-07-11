# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| latest `main` release (`v*`) | :white_check_mark: |
| older releases            | :x:                |

Only the most recent published release receives security fixes.

## Reporting a Vulnerability

If you discover a security vulnerability in ScreenshotJanitor, please **do not
open a public issue**.

Instead, report it privately using GitHub's built-in security advisories:

1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability** (Private Vulnerability Reporting).
3. Provide a description, reproduction steps, and impact.

You can expect an initial response within a few days. Once the issue is
confirmed and fixed, a patched release will be published and you will be
credited (unless you prefer to remain anonymous).

## Notes on Permissions

ScreenshotJanitor requests storage/photo permissions (`READ_MEDIA_IMAGES` /
`READ_EXTERNAL_STORAGE`) and scans the device for screenshots. It does **not**
upload, transmit, or share any user files or media off the device. File access
is limited to local cleanup and archiving operations performed on the user's
own device.
