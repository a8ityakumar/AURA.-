# AURA.

AURA. is an Android-first AI assistant prototype built with Google AI Studio.  
The project focuses on creating a clean, premium, mobile-friendly AI chat experience with strong readability, structured responses, and careful factual behavior.

## Overview

AURA. is designed as a minimal black-and-white AI assistant interface inspired by modern AI products like ChatGPT, Perplexity, and premium mobile productivity apps.

The goal is not just to build another chatbot, but to create an assistant that feels polished, readable, honest, and useful on Android devices.

## Current Features

- Android-first chat UI
- Minimal black-and-white premium design
- Clean message layout
- Structured AI responses
- Heading hierarchy for better readability
- Mobile-friendly spacing
- User-friendly error handling
- Safer response behavior for unknown or current information
- Anti-hallucination rules for latest/current queries
- Support for analyzing user-provided text, screenshots, and visible profile information

## Current Development Focus

AURA. is currently focused on improving:

- Response accuracy
- Markdown rendering
- Table rendering
- Chat readability
- Stable AI model connection
- Better error handling
- Play Store-ready architecture planning

## Important Design Decisions

AURA. does not pretend to know live or latest information unless online search is properly connected.

If a user asks about current topics such as latest news, social media profiles, prices, server status, exam updates, or recent releases, AURA. clearly explains that it cannot verify current information without online search.

This makes the assistant more trustworthy and reduces hallucinated answers.

## Planned Features

- Proper markdown rendering
- Real table rendering
- Chat history persistence
- Memory system
- Voice input and output
- Streaming responses
- Better message bubbles
- Backend-based API architecture
- Optional online search with verified sources
- Play Store-ready deployment structure

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
