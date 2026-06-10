# AURA.

AURA. is an Android-first AI assistant prototype built with Google AI Studio.

The project focuses on creating a clean, premium, mobile-friendly AI assistant experience with strong readability, structured responses, account support, and careful factual behavior.

## Overview

AURA. is designed as a minimal black-and-white AI assistant interface for Android devices.

The goal of AURA. is not just to be another chatbot, but to feel like a polished AI product with a clean interface, readable responses, honest limitations, and a stable mobile-first experience.

## Current Version

### AURA. v1.1.0 — Account and Stability Update

This version focuses on user accounts, Firebase authentication, profile improvements, login stability, and overall app polish.

## Current Features

* Android-first AI assistant interface
* Minimal black-and-white premium UI
* Firebase Email/Password authentication
* User signup and login flow
* Persistent user session
* User account/profile area
* Profile picture support
* Edit profile structure
* Clean login screen
* Structured AI responses
* Heading hierarchy for better readability
* Mobile-friendly spacing
* Safer behavior for unknown or current information
* User-friendly error handling

## Design Direction

AURA. uses a premium black-and-white visual style with:

* Pure black background
* White primary text
* Soft gray secondary text
* Clean spacing
* Minimal buttons and input fields
* Strong heading hierarchy
* Mobile-first readability

The design avoids unnecessary colors, gradients, heavy effects, and clutter.

## Important AI Behavior

AURA. is designed to avoid overconfident or fake answers.

If the user asks about current, latest, live, or recently changing information, AURA. should not guess unless online search is properly connected.

For topics like news, prices, social media profiles, server status, exam updates, public figures, or recent releases, AURA. should clearly explain that it cannot verify current information without online search or user-provided sources.

## Current Limitations

* Online search is currently disabled
* Google Sign-In is temporarily disabled
* Latest/current information cannot be verified live
* Backend-based internet connectivity is planned for a future version
* Markdown and table rendering are still being improved
* The project is still in prototype development

## Planned Features

* Proper markdown rendering
* Real table rendering
* Chat history persistence
* Memory system
* Voice input and output
* Streaming responses
* Better message bubbles
* Google Sign-In support
* Backend-based API architecture
* Optional online search with verified sources
* Play Store-ready deployment structure

## Tech Direction

Planned architecture:

```text
AURA Android App
↓
Backend / Firebase Cloud Function
↓
AI Model API
↓
AURA UI
```

This structure is planned to keep API keys secure and avoid exposing sensitive credentials inside the Android app.

## Version Roadmap

### v1.0.0

Initial public prototype release.

### v1.1.0

Account and stability update with Firebase Email/Password authentication, profile support, and improved login UI.

### v1.2.0

Planned online intelligence update with backend API setup, source-based search, and verified current information.

## Project Status

AURA. is currently in active prototype development.

The current focus is to make the app stable, polished, and portfolio-ready before adding advanced online intelligence features.

## Creator

AURA. was created and developed by Aks / Aditya.

Online handle: @adityaartificial0078

## Copyright

Copyright © 2026 Aks / Aditya. All rights reserved.

AURA. is a personal AI assistant prototype created and maintained by Aks / Aditya.

Unauthorized copying, redistribution, resale, or claiming this project as someone else’s work is not allowed without permission.
