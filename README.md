⚡ Nord Pool – 1h Prices App

A modern Android app built with Jetpack Compose that visualizes and tracks Nord Pool hourly electricity prices in a simple, intuitive interface.
The app provides both a visual price trend chart and a detailed hourly list, allowing users to easily spot cheap or expensive energy hours.

🧭 Features
📈 Price Chart

Real-time 24-hour line chart with dynamic colors based on price levels.

Automatically adapts to today and tomorrow’s prices.

A thin red vertical line marks the current hour in real time.

Grid lines and labels for easy visual interpretation.



🕒 Price List

Displays hourly electricity prices in a clean two-column layout (Today & Tomorrow).

Each row shows:

Hour range (e.g. 19:00 – 20:00)

A bell icon to enable or disable notifications

The hourly price, color-coded by level:

💚 Low (below 0.10 €/kWh)

💛 Moderate (0.10 – 0.20 €/kWh)

❤️ High (above 0.20 €/kWh)

Automatically hides past hours.

Highlights the current hour in red.



🔔 Notifications

Users can tap the bell icon to schedule a reminder for upcoming low-price hours.

Notifications trigger 10 minutes before the selected time.

Works even if the app is in the background.



🪶 Interface

Built with Jetpack Compose (Material 3) for a modern, responsive UI.

Consistent green theme inspired by energy efficiency.

Smooth padding and typography for visual clarity.



🧩 Tech Stack
Component	Technology
UI Framework	Jetpack Compose (Material 3)
Networking	OkHttp
Data Parsing	Kotlin CSV
Chart Rendering	Custom Canvas drawing (Compose)
Notifications	Android AlarmManager + BroadcastReceiver
Language	Kotlin
Minimum SDK	24 (Android 7.0 Nougat)
Target SDK	34 (Android 14)


⚙️ How It Works

On app launch, the data is fetched from the Nord Pool hourly API (via CSV).

Prices are grouped by date (Today & Tomorrow).

The app renders:

The PriceChart, showing visual price trends.

The PriceList, showing hourly values with interactive notifications.

The user can tap the bell icon for a specific hour to schedule an alert.