# Mollat Android App

An isolated Android application shell for the existing Leave Management System.

- The live GitHub Pages website source and deployment branch are not modified.
- The app uses the existing production website and Supabase authentication/database.
- Android adds a mobile-first visual layer, native print handoff, file picker, downloads, pull-to-refresh, and back navigation.
- No Supabase service-role key is included.

GitHub Actions builds an installable debug APK on pushes to the android-app branch. Download the mollat-android-apk artifact from the workflow run.
