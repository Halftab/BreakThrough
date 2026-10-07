# Break Through

Break Through is a small, offline 2D Android game. The player guides Guy through a side-scrolling course, shooting breakable glass barriers while avoiding rigid walls.

## License

The game source code and original project artwork/audio created for Break Through are released under the Apache License 2.0. See `LICENSE`.

## Third-party assets

- **Glass-breaking sound:** Rosebugg (Freesound), distributed via Pixabay as "Glass Breaking"; the public listing states that it is free to use under the Pixabay Content License.
- **Pistol artwork:** FreeSVG / OpenClipart, public domain / CC0 according to the source listing.
- **Original Flaticon hoverboard:** not redistributed by this F-Droid preparation build. It has been replaced by an original project-created hoverboard asset under Apache-2.0.

Full provenance and attribution details are in `THIRD_PARTY_ASSETS.md`.

## Build

The Android project is a native WebView wrapper around the self-contained `www/index.html` game. It does not require Node.js, npm, Capacitor, Google Play Services, Firebase, analytics, advertising, or a network connection at runtime.

Build the `release` variant with Gradle.
