# Why another MediaPlayer?

I just recently found a video player app I used got bought and turned into ad infused spyware so this is an attempt to write a video player that hopefully does what I want and nothing more.

# High Level Design

Plan changed, now trying with compose so there are a few `@Composables` hosted in MainActivity via `setContent {}`
- `MainScreen` shows button to launch SAF file picker then launch ExoPlayer
- `ExoPlayerScreen` hosts StyledPlayerView that talk to ExoPlayer instance created by ExoHolderVM 

ViewModels hosted in MainActivity
- `ExoHolderVM`: creates and holds a instance of `ExoPlayer` object that is exposed to `ExoPlayerScreen`


## Flow to open a local file

compose callback to main viewmodel, which then have handler in MainActivity to launch SAF via Intent
In onActivityResult the resulting intent is passed to viewmodel, which then emits a different state
that is handled in compose; this handler then passes uri to exo holder and then launches 
`ExoPlayerScreen` with no arguments.


# TODOs

## Feature

[] Support continued playback with device rotation

    []  Figure out how to un-assign player from `StyledVideoView` so that we don't
        have to always release player

[] Add UI to adjust volume and brightness 

## Bugs/Issues

[] Cannot play .ts from Synology DSFiles app

    [] might be that exoplayer doesn't support mpeg2-ts over pure http get?
    [] for some reason, it works on Amazon Fire HD10+?


# Work Log

2022-04-11  stuck between state emitted via viewmodel and navigation controller that is created in setContent
            maybe it's just too much abstraction to have a viewmodel here?

2022-10-11  refactored to use navigation to parse launching intent and pass to exoplayer screen
            create exo holder view model to hold exo player object

2022-10-16  Got local file play kind of working 
