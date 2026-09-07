# Android Auto Clicker

An Android automation application for creating reusable tap and swipe sequences.

## Features

* Add multiple tap points
* Create swipe actions using start and end points
* Adjust the time interval between actions
* Set a specific repeat count
* Use continuous repetition mode
* Start and stop automation using floating controls
* Move the floating control around the screen
* Save, load, rename, overwrite, and delete profiles
* Automatically stop automation when the floating control is closed

## Technologies Used

* Kotlin
* Android Studio
* Jetpack Compose
* Android Accessibility Service
* Android Overlay Service
* Gradle

## How It Works

The application uses Android's Accessibility Service to perform user-created tap and swipe actions. Floating controls allow the user to start or stop an automation while using another application.

The user must manually enable the Accessibility Service and allow the display-over-other-apps permission.

## Testing

The application was built and tested on a physical OnePlus Android device. Tap sequences, swipe actions, repeat mode, continuous mode, floating controls, and saved profiles were tested.

## Responsible Use

This project is intended for personal automation, accessibility, learning, and testing purposes. Users are responsible for following the rules and terms of any application in which they use automation.

Do not use this application for fraud, spam, unauthorized activity, bypassing security protections, or causing harm.

## AI-Assisted Development

This project was developed with assistance from ChatGPT.

I defined the application requirements, configured the Android Studio project, tested the application on a physical device, identified problems, and verified the fixes. ChatGPT assisted with explaining concepts, generating code, troubleshooting errors, and improving the implementation.

I am continuing to learn Kotlin, Android development, Git, and GitHub through this project.

## Repository Security

Generated APK files, build folders, local configuration files, signing keys, and passwords are excluded from this repository.

## Status

Personal learning project — development and testing are ongoing.
