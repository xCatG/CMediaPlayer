# Why another MediaPlayer?

I just recently found a video player app I used got bought and turned into ad infused spyware so this is an attempt to write a video player that hopefully does what I want and nothing more.

Plan changed, now trying with compose so there are a few `@Composables` hosted in MainActivity via `setContent {}`
- `MainScreen` shows button to launch SAF file picker then launch ExoPlayer
- `ExoPlayerScreen` hosts StyledPlayerView that talk to ExoPlayer instance created by ExoHolderVM 

ViewModels hosted in MainActivity
- `ExoHolderVM`: creates and holds a instance of `ExoPlayer` object that is exposed to `ExoPlayerScreen`

# High Level Design

TBD

# TODOs

Feature
[] Support continued playback with device rotation
[] Support open local files via SAF
[] Add UI to adjust volume and brightness 

Bugs/Issues
[] Cannot play .ts from Synology DSFiles app
    [] might be that exoplayer doesn't support mpeg2-ts over pure http get?


# Work Log

2022-04-11  stuck between state emitted via viewmodel and navigation controller that is created in setContent
            maybe it's just too much abstraction to have a viewmodel here?

2022-10-11  refactored to use navigation to parse launching intent and pass to exoplayer screen
            create exo holder view model to hold exo player object
2022-10-16  trying to tackle TODOs and Bugs