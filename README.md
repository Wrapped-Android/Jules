<div align="center">
  <h1>Jules Android Wrapper 🐙</h1>

  [![Download Jules APK](https://img.shields.io/badge/Download-Jules.apk-715cd7?style=for-the-badge&logo=android)](https://github.com/Wrapped-Android/Jules/releases/latest/download/Jules.apk)

  A polished, semi-native Android application that wraps the [Jules](https://jules.google.com) website into a high-performance, full-screen mobile experience.
</div>

## ✨ Key Features

- **Full-Screen Immersion**: True edge-to-edge display using Android's `enableEdgeToEdge`, respecting system insets for a native look.
- **Native Gestures**: Bidirectional swipe detection (right to open, left to close) to control the web sidebar seamlessly.
- **Session Persistence**: Robust cookie synchronization ensures you stay logged in permanently.
- **Theme Synchronization**: The app's appearance (Light/Dark mode) automatically follows your Android system settings in real-time.
- **UI Optimization**:
    - Removed redundant web elements (logo, update buttons, footers) for a cleaner mobile view.
    - Refined navbar layout with optimized spacing and alignment.
    - Removed the "web-like" blue tap highlight for a more immediate, native feel.
- **Custom Branding**: Modern adaptive icon featuring the octopus logo on a professional dark-gray background.

## 🛠️ Technical Implementation

- **Language**: Kotlin (Jetpack Compose)
- **Engine**: Android WebView with custom JavaScript/CSS injection.
- **Bridge**: Real-time communication between Android system theme and web interface.
- **CI/CD**: Automated release workflow with version tagging.

## 📄 License

This project is a wrapper for a Google-owned service. Ensure compliance with their Terms of Service when using this application.
