# EV Charging Station Booking (Android Kotlin)

A clean, modular Android app (Kotlin) for EV owners to manage reservations and for station operators to scan/confirm bookings.

## Architecture

- MVVM with Repositories and Retrofit for network calls
- SQLiteOpenHelper for local EV Owner user storage (NIC as PK)
- ZXing for QR generation/scanning
- Google Maps SDK to show nearby charging stations
- Material Components for modern UI

## Features

- EV Owner:
  - Register/Update/Deactivate account (local SQLite)
  - Create/Modify/Cancel reservations (12h rule enforced locally)
  - View upcoming and history
  - Dashboard showing counts + map with nearby stations
- Station Operator:
  - Login (C# Web API)
  - Scan QR to retrieve booking
  - Confirm booking to finalize session

## Setup

1. Android Studio Iguana/Koala, JDK 17, Android SDK 34
2. Add your Google Maps API key to AndroidManifest:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_GOOGLE_MAPS_API_KEY" />
   ```
3. Update `RetrofitClient.BASE_URL` to your C# Web API base URL
4. Align DTOs in `model/ApiModels.kt` and endpoints in `network/ApiService.kt`
5. Run on a device with Google Play services for Maps & Location

## Notes

- Package name: `com.example.evcharger`
- No external frameworks beyond SDK and allowed libraries (Retrofit, ZXing, Material, Google Maps)
- JavaDoc-style comments and block comments added
- Extend layouts and add navigation as needed